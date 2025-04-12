package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.CallResult;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serial;

public class IBMiCommandStep extends IBMiStep<CallResult> {
	@Serial
	private static final long serialVersionUID = 6443392002952411163L;

	private final String command;
	private boolean failOnError = true;

	@DataBoundConstructor
	public IBMiCommandStep(final String command) {
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
	protected CallResult runOnIBMi(final StepContext context, final LoggerWrapper logger, final IBMi ibmi) throws AS400SecurityException, IOException, InterruptedException, ErrorCompletingRequestException {
		logger.log(Messages.IBMICommandStep_running(command));

		final CallResult result = ibmi.executeCommand(command);
		final AS400Message lastMessage = result.getLastMessage();
		if (result.isSuccessful()) {
			logger.log(Messages.IBMICommandStep_succeeded(command));
		} else {
			final String error;
			if (lastMessage != null) {
				error = Messages.IBMICommandStep_failed_with_message(command, lastMessage.getID(), lastMessage.getText());
			} else {
				error = Messages.IBMICommandStep_failed(command);
			}
			if (!failOnError) {
				logger.error(error);
				logger.trace(result.getPrettyMessages());
			} else {
				logger.error(Messages.IBMICommandStep_failed(command) + "\n" + result.getPrettyMessages("\t"));
				throw new AbortException(error);
			}
		}

		return result;
	}

	@Extension
	public static class DescriptorImpl extends IBMiStepDescriptor {
		@Override
		public String getFunctionName() {
			return "ibmiCommand";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.IBMICommandStep_description();
		}
	}
}
