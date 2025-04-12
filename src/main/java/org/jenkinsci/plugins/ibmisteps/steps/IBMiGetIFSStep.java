package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.IFSFile;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serial;
import java.text.MessageFormat;

public class IBMiGetIFSStep extends IBMiStep<Void> {
	@Serial
	private static final long serialVersionUID = -6721839340320567902L;

	private final String from;
	private final String to;

	@DataBoundConstructor
	public IBMiGetIFSStep(final String from, final String to) {
		this.from = from;
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	@Override
	protected Void runOnIBMi(final StepContext context, final LoggerWrapper logger, final IBMi ibmi) throws IOException, InterruptedException, AS400SecurityException {
		final IFSFile fromIFS = new IFSFile(ibmi.getIbmiConnection(), from);
		if (!fromIFS.exists()) {
			throw new AbortException(Messages.IBMiGetIFSStep_from_not_found(fromIFS));
		}

		final FilePath toPath = context.get(FilePath.class).child(to);
		if (!toPath.exists()) {
			toPath.mkdirs();
			if (!toPath.exists()) {
				throw new AbortException(Messages.IBMiGetIFSStep_failed_create_to(toPath));
			}
		} else if (!toPath.isDirectory()) {
			throw new AbortException(Messages.IBMiGetIFSStep_to_is_file(toPath));
		}

		if (fromIFS.isDirectory()) {
			logger.log(Messages.IBMiGetIFSStep_copy_folder(fromIFS, toPath));
			getFolder(logger, ibmi, fromIFS, toPath);
		} else if (fromIFS.isFile()) {
			logger.log(Messages.IBMiGetIFSStep_copy_file(fromIFS, toPath));
			getFile(logger, ibmi, fromIFS, toPath);
		}

		return null;
	}

	private void getFile(final LoggerWrapper logger, final IBMi ibmi, final IFSFile ifsFile, final FilePath folder)
			throws IOException, AS400SecurityException, InterruptedException {
		final FilePath targetFile = folder.child(ifsFile.getName());
		if (targetFile.exists()) {
			targetFile.delete();
		}

		logger.trace(MessageFormat.format("Getting {0} into {1} ({2} bytes)", ifsFile, targetFile, ifsFile.length()));
		ibmi.download(ifsFile, targetFile);
	}

	private void getFolder(final LoggerWrapper logger, final IBMi ibmi, final IFSFile ifsFolder,
	                       final FilePath folder) throws IOException, InterruptedException, AS400SecurityException {
		ifsFolder.setPatternMatching(IFSFile.PATTERN_POSIX_ALL);
		for (final IFSFile item : ifsFolder.listFiles()) {
			if (item.isDirectory()) {
				getFolder(logger, ibmi, item, folder.child(item.getName()));
			} else if (item.isFile()) {
				getFile(logger, ibmi, item, folder);
			}
		}
	}

	@Extension
	public static class DescriptorImpl extends IBMiStepDescriptor {
		@Override
		public String getFunctionName() {
			return "ibmiGetIFS";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.IBMiGetIFSStep_description();
		}
	}
}
