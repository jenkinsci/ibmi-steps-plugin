package org.jenkinsci.plugins.ibmisteps.steps;

import com.ibm.as400.access.AS400JDBCStatement;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.LoggerWrapper;
import org.jenkinsci.plugins.ibmisteps.model.SQLResult;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStep;
import org.jenkinsci.plugins.ibmisteps.steps.abstracts.IBMiStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serial;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IBMiRunSQLStep extends IBMiStep<SQLResult> {
	@Serial
	private static final long serialVersionUID = 5802097350903272246L;

	private final String sql;

	@DataBoundConstructor
	public IBMiRunSQLStep(final String sql) {
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}

	@Override
	protected SQLResult runOnIBMi(final StepContext context, final LoggerWrapper logger, final IBMi ibmi) throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException, ErrorCompletingRequestException {
		logger.log(Messages.IBMiRunSQLStep_running(sql));

		try (AS400JDBCStatement statement = ibmi.getDB2Statement()) {
			final SQLResult result;
			if (statement.execute(sql)) {
				final ResultSet resultSet = statement.getResultSet();
				result = new SQLResult(resultSet);
				logger.trace(Messages.IBMiRunSQLStep_rows(result.getRowCount()));

			} else {
				result = new SQLResult(statement.getUpdateCount());
				logger.trace(Messages.IBMiRunSQLStep_updated(result.getUpdateCount()));
			}
			return result;
		} catch (final SQLException e) {
			logger.error(Messages.IBMiRunSQLStep_failed(e.getLocalizedMessage()));
			throw e;
		}
	}

	@Extension
	public static class DescriptorImpl extends IBMiStepDescriptor {
		@Override
		public String getFunctionName() {
			return "ibmiRunSQL";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.IBMiRunSQLStep_description();
		}
	}
}
