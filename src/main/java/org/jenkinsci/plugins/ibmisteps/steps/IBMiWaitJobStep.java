package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.*;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class IBMiWaitJobStep extends IBMiStep<Job> {
	private static final long serialVersionUID = 6918372653694281442L;

	private final String name;
	private final String number;
	private final String user;

	private int timeout;

	@DataBoundConstructor
	public IBMiWaitJobStep(final String name, final String number, final String user) {
		this.name = name;
		this.number = number;
		this.user = user;
	}

	@DataBoundSetter
	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	public String getName() {
		return name;
	}

	public String getUser() {
		return user;
	}

	public String getNumber() {
		return number;
	}

	public int getTimeout() {
		return timeout;
	}

	@Extension
	public static class DescritptorImpl extends IBMiStepDescriptor {
		@Override
		public String getFunctionName() {
			return "ibmiWaitJob";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.IBMiWaitJob_description();
		}
	}

	@Override
	protected Job runOnIBMi(final StepContext stepContext, final LoggerWrapper logger, final IBMi ibmi) throws InterruptedException {
		final Instant start = Instant.now();
		final Job job = new Job(ibmi.getIbmiConnection(), name, user, number);
		logger.log(Messages.IBMiWaitJob_waiting(job.toString()));
		boolean timeoutReached = false;
		try {
			while (isJobRunning(job) && !timeoutReached) {
				timeoutReached = (timeout > 0 && Duration.between(start, Instant.now()).getSeconds() > timeout);
				Thread.sleep(500);
			}
		} catch (AS400SecurityException | IOException | ObjectDoesNotExistException |
		         ErrorCompletingRequestException e) {
			logger.log(Messages.IBMiWaitJob_error(e.getLocalizedMessage()));
		}

		if (timeoutReached) {
			logger.log(Messages.IBMiWaitJob_timeout_reached(timeout));
		} else {
			logger.log(Messages.IBMiWaitJob_job_ended(job.toString()));
		}

		return job;
	}

	private boolean isJobRunning(Job job) throws AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException, ErrorCompletingRequestException {
		try {
			job.loadInformation();
			return job.getStatus().equals(Job.JOB_STATUS_ACTIVE);
		} catch (AS400Exception e) {
			// Jobs created by SBMJOB tend to get cleaned-up immediately upon job completion,
			// whereupon getStatus(), getCompletionStatus(), and most other 'get' methods will throw an AS400Exception containing an AS400Message
			// indicating "Internal job identifier no longer valid" (message ID CPF3C52).
			// That exception should be interpreted as an indication that the job has completed.
			if (e.getAS400Message().getID().equals("CPF3C52")) {
				return true;
			} else {
				throw e;
			}
		}
	}
}
