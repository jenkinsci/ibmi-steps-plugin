package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.model.ShellExec;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serial;

public class IBMiShellExecStep extends IBMiStep<ShellExec> {
	@Serial
	private static final long serialVersionUID = 1293378013455000563L;

	private final String command;
	private boolean failOnError = true;

	@DataBoundConstructor
	public IBMiShellExecStep(final String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public boolean isFailOnError() {
		return failOnError;
	}

	@DataBoundSetter
	public void setFailOnError(final boolean failOnError) {
		this.failOnError = failOnError;
	}

	@Override
	protected ShellExec runOnIBMi(final StepContext context, final LoggerWrapper logger, final IBMi ibmi) throws AS400SecurityException, IOException, InterruptedException, ErrorCompletingRequestException {
		logger.log(Messages.IBMiShellExecStep_running(command));

		final ShellExec result = ibmi.executeShellCommand(command);
		if (result.code() == 0) {
			logger.log(Messages.IBMiShellExecStep_succeeded(command));
		} else {
			final String error = Messages.IBMiShellExecStep_failed(command, result.code(), result.output());
			if (failOnError) {
				throw new AbortException(error);
			}
			else{
				logger.error(error);
			}
		}

		return result;
	}

	@Extension
	public static class DescriptorImpl extends IBMiStepDescriptor {
		@Override
		public String getFunctionName() {
			return "ibmiShellExec";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.IBMiShellExecStep_description();
		}
	}
}
