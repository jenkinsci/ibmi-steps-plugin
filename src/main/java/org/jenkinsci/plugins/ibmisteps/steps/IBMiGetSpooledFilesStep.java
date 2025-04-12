package org.jenkinsci.plugins.ibmisteps.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.model.SpooledFileHandler;
import org.jenkinsci.plugins.ibmisteps.model.SpooledFiles;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serial;
import java.text.MessageFormat;

public class IBMiGetSpooledFilesStep extends IBMiStep<SpooledFiles> {
	@Serial
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
		final SpooledFileHandler spooledFileHandler = ibmi.getSpooledFileHandler();
		final SpooledFiles spooledFiles = spooledFileHandler.listSpooledFiles(ibmi, jobNumber, jobUser, jobName);

		final FilePath toFolder = stepContext.get(FilePath.class).child(to);
		if (toFolder.exists() && clearTo) {
			toFolder.deleteContents();
			logger.trace("Cleared target directory %s", toFolder);
		}
		toFolder.mkdirs();

		for (final SpooledFiles.SpooledFile spooledFile : spooledFiles) {
			final FilePath toFile = toFolder.child(spooledFile.getFileName());
			logger.trace(MessageFormat.format("Writing {0} ({1}) into {2}", spooledFile.getName(), spooledFile.getNumber(), toFile));
			spooledFileHandler.writeSpooledFile(ibmi, spooledFile, toFile);
		}

		logger.log(Messages.IBMiGetSpooledFiles_count(spooledFiles.size(), toFolder));
		return spooledFiles;
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
