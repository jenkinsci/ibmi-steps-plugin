package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.model.SpooledFiles;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class IBMiGetSpooledFilesStep extends IBMiStep<SpooledFiles> {
	private static final long serialVersionUID = 1880307039400864220L;

	private final String jobName;
	private final String jobNumber;
	private final String jobUser;
	private final String to;
	private boolean clearTo;

	@DataBoundConstructor
	public IBMiGetSpooledFilesStep(final String jobName, final String jobNumber, final String jobUser,
	                               final String to) {
		this.jobName = jobName.trim().toUpperCase();
		this.jobUser = jobUser.trim().toUpperCase();
		this.jobNumber = jobNumber.trim().toUpperCase();
		this.to = to;
	}

	public String getJobName() {
		return jobName;
	}

	public String getJobNumber() {
		return jobNumber;
	}

	public String getJobUser() {
		return jobUser;
	}

	public String getTo() {
		return to;
	}

	public boolean isClearTo() {
		return clearTo;
	}

	@DataBoundSetter
	public void setClearTo(final boolean clearTo) {
		this.clearTo = clearTo;
	}

	@Override
	protected SpooledFiles runOnIBMi(final StepContext stepContext, final LoggerWrapper logger, final IBMi ibmi)
			throws Exception {
		logger.log(Messages.IBMiGetSpooledFiles_getting(jobNumber, jobUser, jobName));
		final SpooledFiles spooledFiles = listSpooledFiles(ibmi);

		final FilePath toFolder = stepContext.get(FilePath.class).child(to);
		if (toFolder.exists() && clearTo) {
			toFolder.deleteContents();
			logger.trace("Cleared target directory %s", toFolder);
		}
		toFolder.mkdirs();

		for (final SpooledFiles.SpooledFile spooledFile : spooledFiles) {
			final FilePath toFile = toFolder.child(spooledFile.getFileName());
			logger.trace(MessageFormat.format("Writing {0} ({1}) into {2}", spooledFile.getName(), spooledFile.getNumber(), toFile));
			toFile.write(readSpooledFile(ibmi, spooledFile), StandardCharsets.UTF_8.name());
		}

		logger.log(Messages.IBMiGetSpooledFiles_count(spooledFiles.size(), toFolder));
		return spooledFiles;
	}

	private SpooledFiles listSpooledFiles(final IBMi ibmi)
			throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException,
			ErrorCompletingRequestException {
		final SpooledFiles spooledFiles = new SpooledFiles();
		ibmi.executeAndProcessQuery(String.format(
						"Select SPOOLED_FILE_NAME, SPOOLED_FILE_NUMBER, SIZE, USER_DATA, JOB_NAME, JOB_USER, JOB_NUMBER " +
								"From Table(QSYS2.SPOOLED_FILE_INFO(JOB_NAME => '%s/%s/%s', STATUS => '*READY'))",
						jobNumber, jobUser, jobName),
				row -> {
					final SpooledFiles.SpooledFile spooledFile = new SpooledFiles.SpooledFile(
							row.getString("SPOOLED_FILE_NAME"),
							row.getInt("SPOOLED_FILE_NUMBER"),
							row.getLong("SIZE"), row.getString("USER_DATA"),
							row.getString("JOB_NAME"), row.getString("JOB_USER"), row.getString("JOB_NUMBER"));

					if (spooledFile.exists()) {
						spooledFiles.add(spooledFile);
					}
				});

		return spooledFiles;
	}

	private String readSpooledFile(final IBMi ibmi, final SpooledFiles.SpooledFile spooledFile) throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException, ErrorCompletingRequestException {
		final List<String> lines = new ArrayList<>();
		ibmi.executeAndProcessQuery(String.format("select RTRIM(SPOOLED_DATA) from table(SYSTOOLS.spooled_file_data(job_name => '%s/%s/%s', SPOOLED_FILE_NAME => '%s', SPOOLED_FILE_NUMBER => %s))",
						spooledFile.getJobNumber(), spooledFile.getJobUser(), spooledFile.getJobName(),
						spooledFile.getName(), spooledFile.getNumber()),
				row -> lines.add(row.getString(1)));

		return String.join("\n", lines);
	}

	@Extension
	public static class DescriptorImpl extends IBMiStepDescriptor {

		@Override
		public String getFunctionName() {
			return "ibmiGetSPLF";
		}

		@Override
		@NonNull
		public String getDisplayName() {
			return Messages.IBMiGetSpooledFiles_description();
		}
	}
}
