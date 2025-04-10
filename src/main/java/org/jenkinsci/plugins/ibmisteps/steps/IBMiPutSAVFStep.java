package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
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

public class IBMiPutSAVFStep extends IBMiStep<SaveFileContent> {
    private static final long serialVersionUID = 8285322833287436551L;

    private final String library;
    private final String name;
    private final String fromFile;

    @DataBoundConstructor
    public IBMiPutSAVFStep(final String library, final String name, final String fromFile) {
        this.fromFile = fromFile;
        this.library = library.trim().toUpperCase();
        this.name = name.trim().toUpperCase();
    }

    public String getFromFile() {
        return fromFile;
    }

    public String getLibrary() {
        return library;
    }

    public String getName() {
        return name;
    }

    @Override
    protected SaveFileContent runOnIBMi(final StepContext context, final LoggerWrapper logger, final IBMi ibmi)
            throws Exception {
        final FilePath workspaceFile = context.get(FilePath.class).child(fromFile);
        if (!workspaceFile.exists()) {
            throw new AbortException(Messages.IBMiUploadSAVF_workspace_file_not_found(workspaceFile));
        }
        logger.log(Messages.IBMiUploadSAVF_uploading(fromFile, library, name, workspaceFile.length()));

        final SaveFile saveFile = new SaveFile(ibmi.getIbmiConnection(), library, name);
        ibmi.withTempFile(tempFile -> {
            try {
                logger.trace("Uploading %s to %s", workspaceFile, tempFile);
                ibmi.upload(workspaceFile, tempFile);

                final String copyCommand = String.format("CPYFRMSTMF FROMSTMF('%s') TOMBR('%s') MBROPT(*REPLACE)",
                        tempFile.getAbsolutePath(), saveFile.getPath());
                logger.trace("Running " + copyCommand);

                final CallResult copyResult = ibmi.executeCommand(copyCommand);
                if (!copyResult.isSuccessful()) {
                    throw new AbortException(Messages.IBMiUploadSAVF_CPYFRMSTMF_failed(tempFile, library, name,
                            copyResult.getPrettyMessages()));
                }
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
            return "ibmiPutSAVF";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.IBMiUploadSAVF_description();
        }
    }
}
