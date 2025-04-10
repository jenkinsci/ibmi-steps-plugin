package org.jenkinsci.plugins.ibmisteps.model;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.ibm.as400.access.*;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.FilePath;
import hudson.Util;
import hudson.util.Secret;
import org.jenkinsci.plugins.ibmisteps.Messages;

import java.beans.PropertyVetoException;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class IBMi implements ConnectionListener, AutoCloseable, Serializable {
	private static final long serialVersionUID = -3164250407732394897L;

	public static final String SYSBAS = "*SYSBAS";
	private final AS400 ibmiConnection;
	private final transient LoggerWrapper logger;

	private transient Consumer<ConnectionEvent> onConnected;
	private transient Consumer<ConnectionEvent> onDisconnected;

	private transient CharConverter charConverter;
	private int connectionCCSID;

	private String iASP = SYSBAS;
	private transient Job commandJob;
	private transient Job databaseJob;
	private transient AS400JDBCConnection sqlConnection;

	private SpooledFileHandler spooledFileHandler;

	public IBMi(final PrintStream stream, final String host, final StandardUsernamePasswordCredentials credentials,
	            final int ccsid, final boolean secure, final boolean doTrace) throws IOException, InterruptedException {
		logger = new LoggerWrapper(stream, doTrace);
		ibmiConnection = secure ? new SecureAS400() : new AS400();
		try {
			connect(host, credentials, ccsid);
		} catch (final InterruptedException e) {
			throw e;
		} catch (final Exception e) {
			throw new IOException(Messages.IBMi_connection_failed(e.toString()), e);
		}
	}

	private void connect(final String host, final StandardUsernamePasswordCredentials credentials, final int ccsid)
			throws IOException, PropertyVetoException, AS400SecurityException, ObjectDoesNotExistException,
			InterruptedException, ErrorCompletingRequestException {
		ibmiConnection.setGuiAvailable(false);

		if (host != null && !host.isBlank()) {
			logger.trace(Messages.IBMi_connect_remote(host, credentials.getUsername()));
			ibmiConnection.setSystemName(host);
			ibmiConnection.setUserId(credentials.getUsername());
			ibmiConnection.setPassword(Secret.toString(credentials.getPassword()).toCharArray());
		} else {
			logger.trace(Messages.IBMi_connect_local());
		}

		connectionCCSID = ccsid > 0 ? ccsid : getPreferredCCSID();
		if (connectionCCSID < 1 || connectionCCSID > 65535) {
			throw new IOException(Messages.IBMi_connect_invalid_ccsid(connectionCCSID));
		}
		ibmiConnection.setCcsid(connectionCCSID);
		charConverter = new CharConverter(connectionCCSID, ibmiConnection);

		ibmiConnection.connectService(AS400.COMMAND);
		commandJob = ibmiConnection.getJobs(AS400.COMMAND)[0];
		logger.trace("Command job is %s", commandJob);

		setJobCCSID(commandJob);
		setJobInquiryReply(commandJob);

		logger.log(Messages.IBMi_connected(ibmiConnection.getSystemName(),
				getOSVersion(),
				ibmiConnection.getUserId(),
				connectionCCSID,
				ibmiConnection instanceof SecureAS400 ? Messages.using_ssl() : ""));
	}

	private void createSQLConnection() throws SQLException, AS400SecurityException, ObjectDoesNotExistException,
			IOException, InterruptedException, ErrorCompletingRequestException {
		closeSQLConnection();

		logger.trace("Opening SQL connection");
		final Properties properties = new Properties();
		properties.put("naming", "system");
		properties.put("prompt", "false");
		properties.put("big decimal", "false");
		if (connectionCCSID == 1200) {
			properties.put("package ccsid", String.valueOf(connectionCCSID));
		}
		properties.put("translate binary", "true");
		properties.put("keep alive", true);
		properties.put("block size", 512);

		// properties.put("libraries", library); liblist?

		if (ibmiConnection instanceof SecureAS400) {
			properties.put("secure", true);
		}

		if (!isSYSBAS(iASP)) {
			try (final Connection connection = new AS400JDBCDriver().connect(ibmiConnection);
			     final Statement statement = connection.createStatement()) {
				try (final ResultSet resultSet = statement.executeQuery(String.format(
						"Select RDB_NAME From QSYS2.ASP_INFO Where DEVICE_DESCRIPTION_NAME = '%s' Fetch First row only",
						iASP))) {
					if (resultSet.next()) {
						final String databaseName = resultSet.getString(1);
						logger.trace("Database name for iASP %s is %s", iASP, databaseName);
						properties.put("database name", databaseName);
					}
					logger.log("No RDB_NAME found for DEVICE_DESCRIPTION_NAME '%s'", iASP);
				}
			}
		}

		sqlConnection = (AS400JDBCConnection) new AS400JDBCDriver().connect(ibmiConnection, properties, null);
		sqlConnection.setTransactionIsolation(Connection.TRANSACTION_NONE);

		final String sqlJobIdentifier = sqlConnection.getServerJobIdentifier();
		databaseJob = new Job(ibmiConnection,
				sqlJobIdentifier.substring(0, 10).trim(),
				sqlJobIdentifier.substring(10, 20).trim(),
				sqlJobIdentifier.substring(20, 26).trim());
		logger.trace("SQL job is %s", databaseJob);

		setJobCCSID(databaseJob);
	}

	private void setJobInquiryReply(final Job job) throws AS400SecurityException, ObjectDoesNotExistException,
			IOException, InterruptedException, ErrorCompletingRequestException {
		job.setInquiryMessageReply(Job.INQUIRY_MESSAGE_REPLY_DEFAULT);
		job.commitChanges();
	}

	private void setJobCCSID(final Job job) throws AS400SecurityException, ObjectDoesNotExistException, IOException,
			InterruptedException, ErrorCompletingRequestException {
		job.setCodedCharacterSetID(connectionCCSID);
		job.commitChanges();
	}

	private int getPreferredCCSID() {
		try {
			final int profileCCSID = ibmiConnection.getCcsid();
			logger.trace("Profile CCSID is %s", profileCCSID);
			if (profileCCSID == 5026) {
				// 5026 is a complete mess -> use 5035 instead
				logger.log(Messages.IBMi_connect_ccsid_5026());
				return 5035;
			}
			return profileCCSID;
		} finally {
			ibmiConnection.resetAllServices();
		}
	}

	public void changeIASP(final String targetIASP) throws PropertyVetoException, AS400SecurityException, IOException,
			InterruptedException, ErrorCompletingRequestException {
		if (isSYSBAS(iASP) && !isSYSBAS(targetIASP) || !isSYSBAS(iASP) && isSYSBAS(targetIASP) &&
				!Util.fixNull(targetIASP).equalsIgnoreCase(Util.fixNull(iASP))) {
			logger.trace("Changing iASP from %s to %s", iASP, targetIASP);
			if (isSYSBAS(targetIASP)) {
				ibmiConnection.setIASPGroup("*NONE");
			} else {
				ibmiConnection.setIASPGroup(targetIASP);
				if (!ibmiConnection.aspName.equalsIgnoreCase(targetIASP)) {
					throw new IOException(Messages.IBMi_change_iasp_failed(targetIASP));
				}
			}
			iASP = targetIASP;
		}
	}

	private boolean isSYSBAS(final String targetIASP) {
		return targetIASP == null ||
				targetIASP.isBlank() ||
				targetIASP.equals("1") ||
				targetIASP.equals(SYSBAS);
	}

	@Override
	public void close() {
		disconnect();
	}

	public void disconnect() {
		closeSQLConnection();
		if (ibmiConnection != null) {
			logger.trace("Disconnecting IBM i");
			ibmiConnection.disconnectAllServices();
			ibmiConnection.removeConnectionListener(this);
		}
	}

	private void closeSQLConnection() {
		if (sqlConnection != null) {
			logger.trace("Closing SQL connection");
			try {
				sqlConnection.close();
			} catch (final SQLException e) {
				logger.error(Messages.IBMi_closeSQL_error(e));
			} finally {
				sqlConnection = null;
				databaseJob = null;
			}
		}
	}

	public void onConnected(final Consumer<ConnectionEvent> onConnected) {
		this.onConnected = onConnected;
	}

	public void onDisconnected(final Consumer<ConnectionEvent> onDisconnected) {
		this.onDisconnected = onDisconnected;
	}

	@Override
	public void connected(final ConnectionEvent event) {
		logger.trace("Received connected event for %s", AS400.getServerName(event.getService()));
		if (onConnected != null) {
			onConnected.accept(event);
		}
	}

	@Override
	public void disconnected(final ConnectionEvent event) {
		logger.trace("Received disconnected event for %s", AS400.getServerName(event.getService()));
		if (onDisconnected != null) {
			onDisconnected.accept(event);
		}
	}

	public CharConverter getCharConverter() {
		return charConverter;
	}

	public int getConnectionCCSID() {
		return connectionCCSID;
	}

	public String getiASP() {
		return iASP;
	}

	public Job getCommandJob() {
		return commandJob;
	}

	public Job getDatabaseJob() {
		return databaseJob;
	}

	public AS400JDBCConnection getSqlConnection() throws AS400SecurityException, SQLException,
			ObjectDoesNotExistException, IOException, InterruptedException, ErrorCompletingRequestException {
		if (sqlConnection == null) {
			createSQLConnection();
		}
		return sqlConnection;
	}

	public AS400JDBCStatement getDB2Statement() throws SQLException, AS400SecurityException,
			ObjectDoesNotExistException, IOException, InterruptedException, ErrorCompletingRequestException {
		return (AS400JDBCStatement) getSqlConnection().createStatement();
	}

	/**
	 * @param query        a SQL query
	 * @param rowProcessor a processor that will run a process on each row
	 * @return <code>true</code> if at least one row was processed, <code>false</code> otherwise.
	 * @throws SQLException
	 * @throws AS400SecurityException
	 * @throws ObjectDoesNotExistException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ErrorCompletingRequestException
	 */
	public boolean executeAndProcessQuery(final String query, final RowProcessor rowProcessor)
			throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException, ErrorCompletingRequestException {
		boolean found = false;
		try (final AS400JDBCStatement statement = getDB2Statement()) {
			try (final ResultSet resultSet = statement.executeQuery(query)) {
				while (resultSet.next()) {
					found = true;
					rowProcessor.processRow(resultSet);
				}
			}
		}

		return found;
	}

	public synchronized CallResult executeCommand(@CheckForNull String command)
			throws IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException {
		command = Util.fixNull(command).trim();
		final CommandCall commandCall = new CommandCall(ibmiConnection, command);
		commandCall.setMessageOption(AS400Message.MESSAGE_OPTION_ALL);
		final boolean executionOK = commandCall.run();
		return new CallResult(executionOK, commandCall.getMessageList());
	}

	public String getOSVersion() throws AS400SecurityException, IOException {
		return String.format("%s.%s", ibmiConnection.getVersion(), ibmiConnection.getRelease());
	}

	public AS400 getIbmiConnection() {
		return ibmiConnection;
	}

	/**
	 * Runs a {@link TempFileTask} with a temporary {@link IFSFile} whose name is guaranteed to be unique.
	 *
	 * @param task
	 * @throws IOException
	 */
	public void withTempFile(final TempFileTask task)
			throws IOException, InterruptedException {
		final IFSFile tempFile = new IFSFile(ibmiConnection, "/tmp", UUID.randomUUID() + ".jenkins.temp");
		if (tempFile.exists()) {
			tempFile.delete();
		}

		try {
			task.run(tempFile);
		} finally {
			if (tempFile.exists()) {
				tempFile.delete();
			}
		}
	}

	public long download(final IFSFile from, final FilePath to)
			throws IOException, AS400SecurityException, InterruptedException {
		try (InputStream input = new BufferedInputStream(new IFSFileInputStream(from));
		     OutputStream output = new BufferedOutputStream(to.write())) {
			return copy(input, output);
		}
	}

	public long upload(final FilePath from, final IFSFile to)
			throws IOException, AS400SecurityException, InterruptedException {
		if (to.getParentFile() != null) {
			to.getParentFile().mkdirs();
		}
		try (InputStream input = new BufferedInputStream(from.read());
		     OutputStream output = new BufferedOutputStream(new IFSFileOutputStream(to))) {
			return copy(input, output);
		}
	}

	private long copy(final InputStream input, final OutputStream output) throws IOException {
		final byte[] buffer = new byte[1048576];
		int read = -1;
		long bytes = 0;
		while ((read = input.read(buffer)) > -1) {
			output.write(buffer, 0, read);
			bytes += read;
		}

		return bytes;
	}

	public SpooledFileHandler getSpooledFileHandler() {
		if (spooledFileHandler == null) {
			final AtomicInteger checkCount = new AtomicInteger(0);
			try {
				executeAndProcessQuery("Select count(*) from QSYS2.sysroutines where routine_name in ('SPOOLED_FILE_DATA', 'SPOOLED_FILE_INFO')",
						row -> checkCount.set(row.getInt(1)));
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.log(Messages.IBMi_failed_sql_service_check(e.getLocalizedMessage()));
			} catch (SQLException | AS400SecurityException | ObjectDoesNotExistException | IOException |
			         ErrorCompletingRequestException e) {
				logger.log(Messages.IBMi_failed_sql_service_check(e.getLocalizedMessage()));
			}

			if (checkCount.get() == 2) {
				logger.trace("Using SQL spooled files handler");
				spooledFileHandler = new SQLSpooledFilehandler();
			} else {
				logger.trace("Using CL spooled files handler");
				spooledFileHandler = new CLSpooledFilehandler();
			}
		}
		return spooledFileHandler;
	}
}
