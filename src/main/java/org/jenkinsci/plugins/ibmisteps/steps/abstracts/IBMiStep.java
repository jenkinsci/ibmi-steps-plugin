package org.jenkinsci.plugins.ibmisteps.steps.abstracts;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.list.OpenListException;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.IBMiContext;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;

public abstract class IBMiStep<T> extends Step implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	protected abstract T runOnIBMi(StepContext stepContext, LoggerWrapper logger, IBMi ibmi) throws IOException, InterruptedException, AS400SecurityException, ErrorCompletingRequestException, ObjectDoesNotExistException, SQLException, OpenListException;

	@Override
	public StepExecution start(final StepContext context) {
		return new SynchronousNonBlockingStepExecution<T>(context) {
			@Serial
			private static final long serialVersionUID = 1L;

			private transient LoggerWrapper logger;

			@Override
			protected T run() throws Exception {
				final IBMi ibmi = getContext()
						.get(IBMiContext.class)
						.getIBMi(getContext().get(TaskListener.class));

				return runOnIBMi(getContext(), getLogger(), ibmi);
			}

			private LoggerWrapper getLogger() throws IOException, InterruptedException {
				if (logger == null) {
					final boolean traceEnabled = getContext().get(IBMiContext.class).isTraceEnabled();
					final PrintStream printStream = getContext().get(TaskListener.class).getLogger();
					logger = new LoggerWrapper(printStream, traceEnabled);
				}

				return logger;
			}
		};
	}

}
