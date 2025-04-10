package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import hudson.FilePath;
import org.jenkinsci.plugins.ibmisteps.model.SpooledFiles.SpooledFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLSpooledFilehandler implements SpooledFileHandler {
	private static final long serialVersionUID = -115898412769496093L;

	private static final String SPOOLED_FILE_DATA = """
			Select RTRIM(SPOOLED_DATA)
			From TABLE(SYSTOOLS.SPOOLED_FILE_DATA(JOB_NAME =>'%s/%s/%s', SPOOLED_FILE_NAME =>'%s', SPOOLED_FILE_NUMBER => %d))
			Order By ORDINAL_POSITION
			""";

	private static final String SPOOLED_FILE_INFO = """
			Select SPOOLED_FILE_NAME, SPOOLED_FILE_NUMBER, SIZE, USER_DATA, JOB_NAME, JOB_USER, JOB_NUMBER
			From Table(QSYS2.SPOOLED_FILE_INFO(JOB_NAME => '%s/%s/%s', STATUS => '*READY'))
			""";

	SQLSpooledFilehandler() {

	}

	@Override
	public void writeSpooledFile(final IBMi ibmi, final SpooledFile spooledFile, final FilePath toFile)
			throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException,
			ErrorCompletingRequestException {
		final List<String> content = new ArrayList<>();
		final String query = String.format(SPOOLED_FILE_DATA,
				spooledFile.getJobNumber(),
				spooledFile.getJobUser(),
				spooledFile.getJobName(),
				spooledFile.getName(),
				spooledFile.getNumber());
		ibmi.executeAndProcessQuery(query, row -> content.add(row.getString(1)));
		toFile.write(String.join("\n", content), StandardCharsets.UTF_8.name());
	}

	@Override
	public SpooledFiles listSpooledFiles(final IBMi ibmi, final String jobNumber, final String jobUser, final String jobName)
			throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException,
			ErrorCompletingRequestException {
		final SpooledFiles spooledFiles = new SpooledFiles();
		ibmi.executeAndProcessQuery(String.format(SPOOLED_FILE_INFO,
				jobNumber,
				jobUser,
				jobName),
				row -> spooledFiles.add(new SpooledFile(row.getString("SPOOLED_FILE_NAME"),
						row.getInt("SPOOLED_FILE_NUMBER"),
						row.getLong("SIZE"), row.getString("USER_DATA"),
						row.getString("JOB_NAME"), row.getString("JOB_USER"), row.getString("JOB_NUMBER"))));
		return spooledFiles;
	}

}
