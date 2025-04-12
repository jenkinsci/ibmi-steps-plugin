package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.SaveFile;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.CallResult;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.model.SaveFileContent;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serial;

public class IBMiGetSAVFStep extends IBMiStep<SaveFileContent> {
	@Serial
	private static final long serialVersionUID = 8285322833287436551L;

	private final String library;
	private final String name;
	private final String toFile;

	@DataBoundConstructor
	public IBMiGetSAVFStep(final String library, final String name, final String toFile) {
		this.library = library.trim().toUpperCase();
		this.name = name.trim().toUpperCase();
		this.toFile = toFile;
	}

	public String getLibrary() {
		return library;
	}

	public String getName() {
		return name;
	}

	public String getToFile() {
		return toFile;
	}

	@Override
	protected SaveFileContent runOnIBMi(final StepContext context, final LoggerWrapper logger, final IBMi ibmi) throws AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException, ErrorCompletingRequestException {
		final SaveFile saveFile = new SaveFile(ibmi.getIbmiConnection(), library, name);
		if (!saveFile.exists()) {
			throw new AbortException(Messages.IBMiDownloadSAVF_save_file_not_found(library, name));
		}

		logger.log(Messages.IBMiDownloadSAVF_downloading(library, name, toFile, saveFile.getLength()));
		ibmi.withTempFile(tempFile -> {
			try {
				final String copyCommand = String.format("CPYTOSTMF FROMMBR('%s') TOSTMF('%s') STMFOPT(*REPLACE)",
						saveFile.getPath(), tempFile.getAbsolutePath());
				logger.trace("Running " + copyCommand);
				final CallResult copyResult = ibmi.executeCommand(copyCommand);
				if (!copyResult.isSuccessful()) {
					throw new AbortException(Messages.IBMiDownloadSAVF_CPYTOSTMF_failed(library, name, tempFile,
							copyResult.getPrettyMessages()));
				}

				final FilePath workspaceFile = context.get(FilePath.class).child(toFile);
				logger.trace("Downloading %s to %s", tempFile, workspaceFile);
				ibmi.download(tempFile, workspaceFile);
			} catch (AS400SecurityException | ErrorCompletingRequestException e) {
				throw new IOException(e);
			}
		});

		return new SaveFileContent(saveFile);
	}

	@Extension
	public static class DescriptorImpl extends IBMiStepDescriptor {
		@Override
		public String getFunctionName() {
			return "ibmiGetSAVF";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.IBMiDownloadSAVF_description();
		}
	}
}
