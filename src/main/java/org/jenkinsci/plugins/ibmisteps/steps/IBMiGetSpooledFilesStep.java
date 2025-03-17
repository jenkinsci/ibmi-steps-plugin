package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.ObjectDoesNotExistException;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.CallResult;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.model.SpooledFiles;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.UUID;

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

    @DataBoundSetter
    public void setClearTo(final boolean clearTo) {
        this.clearTo = clearTo;
    }

    public boolean isClearTo() {
        return clearTo;
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

        final IFSFile tempDirectory = new IFSFile(ibmi.getIbmiConnection(),
                "/tmp",
                UUID.randomUUID() + ".jenkins.temp");
        try {
            final IFSFile workDirectory = new IFSFile(tempDirectory, "work");
            workDirectory.mkdirs();
            logger.trace("Created work directory %s", workDirectory);

            for (final SpooledFiles.SpooledFile spooledFile : spooledFiles) {
                final IFSFile workFile = new IFSFile(workDirectory, spooledFile.getFileName());
                final CallResult cpysplf = ibmi
                        .executeCommand(String.format(
                                "CPYSPLF FILE(%s) SPLNBR(%s) TOFILE(*TOSTMF) JOB(%s/%s/%s) TOSTMF('%s/%s')",
                                spooledFile.getName(), spooledFile.getNumber(),
                                spooledFile.getJobNumber(), spooledFile.getJobUser(), spooledFile.getJobName(),
                                workDirectory, spooledFile.getFileName()));
                if (!cpysplf.isSuccessful()) {
                    throw new AbortException(Messages.IBMiGetSpooledFiles_cpysplf_failed(cpysplf.getPrettyMessages()));
                }

                final IFSFile targetFile = new IFSFile(tempDirectory, spooledFile.getFileName());
                final CallResult cpy = ibmi
                        .executeCommand(String.format("CPY OBJ('%s') TOOBJ('%s') TOCCSID(1208) DTAFMT(*TEXT)",
                                workFile, targetFile));
                if (!cpy.isSuccessful()) {
                    throw new AbortException(Messages.IBMiGetSpooledFiles_cpy_failed(cpy.getPrettyMessages()));
                }

                final FilePath toFile = toFolder.child(spooledFile.getFileName());
                logger.trace(MessageFormat.format("Downloading {0} into {1}", targetFile, toFile));
                ibmi.download(targetFile, toFile);
            }
        } finally {
            final CallResult clear = ibmi.executeCommand(String.format("RMVDIR DIR('%s') SUBTREE(*ALL)",
                    tempDirectory.getAbsolutePath()));
            if (!clear.isSuccessful()) {
                logger.trace("Failed to clear temporary directory %s: %s",
                        tempDirectory,
                        clear.getPrettyMessages());
            }
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

    @Extension
    public static class DescriptorImpl extends IBMiStepDescriptor {

        @Override
        public String getFunctionName() {
            return "ibmiGetSPLF";
        }

        @Override
        public String getDisplayName() {
            return Messages.IBMiGetSpooledFiles_description();
        }
    }
}
