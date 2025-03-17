package org.jenkinsci.plugins.ibmisteps.steps;

import java.io.IOException;
import java.text.MessageFormat;

import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.IFSFile;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;

public class IBMiPutIFSStep extends IBMiStep<Void> {
	private static final long serialVersionUID = 1011610851208715193L;

	private final String from;
	private final String to;

	@DataBoundConstructor
	public IBMiPutIFSStep(final String from, final String to) {
		this.from = from;
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	@Extension
	public static class DescriptorImpl extends IBMiStepDescriptor {
		@Override
		public String getFunctionName() {
			return "ibmiPutIFS";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.IBMiPutIFSStep_description();
		}
	}

	@Override
	protected Void runOnIBMi(final StepContext context, final LoggerWrapper logger, final IBMi ibmi)
			throws Exception {
		final FilePath fromPath = context.get(FilePath.class).child(from);
		if (!fromPath.exists()) {
			throw new AbortException(Messages.IBMiPutIFSStep_from_not_found(fromPath));
		}

		final IFSFile toFolder = new IFSFile(ibmi.getIbmiConnection(), to);
		if (!toFolder.exists()) {
			toFolder.mkdirs();
			if (!toFolder.exists()) {
				throw new AbortException(Messages.IBMiPutIFSStep_failed_create_to(fromPath));
			}
		} else if (toFolder.isFile()) {
			throw new AbortException(Messages.IBMiPutIFSStep_to_is_file(fromPath));
		}

		if (fromPath.isDirectory()) {
			logger.log(Messages.IBMiPutIFSStep_copy_folder(fromPath, toFolder));
			putFolder(logger, ibmi, fromPath, toFolder);
		} else {
			logger.log(Messages.IBMiPutIFSStep_copy_file(fromPath, toFolder));
			putFile(logger, ibmi, fromPath, toFolder);
		}

		return null;
	}

	private void putFile(final LoggerWrapper logger, final IBMi ibmi, final FilePath file, final IFSFile ifsFolder)
			throws IOException, AS400SecurityException, InterruptedException {
		final IFSFile targetFile = new IFSFile(ifsFolder, file.getName());
		if (targetFile.exists()) {
			targetFile.delete();
		}

		logger.trace(MessageFormat.format("Putting {0} into {1} ({2} bytes)", file, targetFile, file.length()));
		ibmi.upload(file, targetFile);
	}

	private void putFolder(final LoggerWrapper logger, final IBMi ibmi, final FilePath folder,
			final IFSFile ifsFolder) throws IOException, InterruptedException, AS400SecurityException {
		for (final FilePath item : folder.list()) {
			if (item.isDirectory()) {
				putFolder(logger, ibmi, item, new IFSFile(ifsFolder, item.getName()));
			} else {
				putFile(logger, ibmi, item, ifsFolder);
			}
		}
	}
}
