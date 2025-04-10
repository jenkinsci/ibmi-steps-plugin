package org.jenkinsci.plugins.ibmisteps.steps;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.collect.ImmutableSet;
import com.ibm.as400.access.AS400SecurityException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.configuration.IBMiGlobalConfiguration;
import org.jenkinsci.plugins.ibmisteps.configuration.IBMiServerConfiguration;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.jenkinsci.plugins.ibmisteps.model.IBMiContext;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OnIBMiStep extends Step implements Serializable {
	private static final long serialVersionUID = 4356326103523728526L;

	private final String server;
	private String iasp;
	private boolean traceEnabled;

	@DataBoundConstructor
	public OnIBMiStep(final String server) {
		this.server = server;
	}

	public String getServer() {
		return server;
	}

	public boolean isTraceEnabled() {
		return traceEnabled;
	}

	public String getIasp() {
		return iasp;
	}

	@DataBoundSetter
	public void setTraceEnabled(final boolean traceEnabled) {
		this.traceEnabled = traceEnabled;
	}

	@DataBoundSetter
	public void setIasp(final String iasp) {
		this.iasp = iasp.trim().toUpperCase();
	}

	@Override
	public StepExecution start(final StepContext context) throws Exception {
		return new GeneralNonBlockingStepExecution(context) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean start() throws Exception {
				final IBMiServerConfiguration serverConfig = Util.fixNull(server).isBlank()
						? new IBMiServerConfiguration("localhost", "localhost", null, "", false)
						: IBMiGlobalConfiguration.get().getServer(server);
				if (serverConfig != null) {
					final StandardUsernamePasswordCredentials credentials = serverConfig.getCredentialsId() != null
							? CredentialsProvider.findCredentialById(serverConfig.getCredentialsId(),
									StandardUsernamePasswordCredentials.class,
									getContext().get(Run.class))
							: null;

					final IBMiContext ibmiContext = new IBMiContext(serverConfig.getHost(),
							credentials,
							serverConfig.getCcsidInt(),
							serverConfig.isSecure(),
							iasp,
							traceEnabled);

					final TaskListener taskListener = getContext().get(TaskListener.class);
					final EnvironmentExpander ibmiExpander = new IBMiExpander(ibmiContext.getIBMi(taskListener));
					final EnvironmentExpander expander = EnvironmentExpander.merge(
							getContext().get(EnvironmentExpander.class),
							ibmiExpander);

					if (traceEnabled) {
						taskListener.getLogger().printf("%nIBM i steps environment variables:%n%s%n%n", ibmiExpander);
					}

					getContext().newBodyInvoker()
							.withContexts(ibmiContext, expander)
							.withCallback(new Callback(ibmiContext))
							.start();
				} else {
					throw new IllegalArgumentException(Messages.server_not_found(server));
				}
				return false;
			}

		};
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {
		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return ImmutableSet.of(Run.class, TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "onIBMi";
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.OnIBMiStep_description();
		}

		@Override
		public boolean takesImplicitBlockArgument() {
			return true;
		}

		@Restricted(NoExternalUse.class) // For Snippet Generator
		@RequirePOST
		public ListBoxModel doFillServerItems(@AncestorInPath final Item item) {
			final ListBoxModel servers = new ListBoxModel();
			// servers.add(Messages.OnIBMiStep_localhost(), ""); saved for later
			if (item != null) {
				item.checkPermission(Item.EXTENDED_READ);
				for (final IBMiServerConfiguration server : IBMiGlobalConfiguration.get().getServers()) {
					servers.add(MessageFormat.format("{0} ({1})", server.getName(), server.getHost()),
							server.getName());
				}
			}
			return servers;
		}
	}

	private static class Callback extends BodyExecutionCallback.TailCall {
		private static final long serialVersionUID = 4348731407610313151L;

		private final IBMiContext ibmiContext;

		Callback(final IBMiContext ibmiContext) {
			this.ibmiContext = ibmiContext;
		}

		@Override
		protected void finished(final StepContext context) {
			ibmiContext.close();
		}
	}

	private static class IBMiExpander extends EnvironmentExpander {
		private static final long serialVersionUID = -1512948481734939923L;

		private final Map<String, String> ibmiEnvVars = new HashMap<>();

		public IBMiExpander(final IBMi ibmi) throws AS400SecurityException, IOException {
			ibmiEnvVars.put(key("host"), ibmi.getIbmiConnection().getSystemName());
			ibmiEnvVars.put(key("profile"), ibmi.getIbmiConnection().getUserId());
			ibmiEnvVars.put(key("ccsid"), String.valueOf(ibmi.getConnectionCCSID()));
			ibmiEnvVars.put(key("command_job"), String.valueOf(ibmi.getCommandJob()));
			ibmiEnvVars.put(key("version"), ibmi.getOSVersion());
		}

		private String key(final String name) {
			return String.format("IBMI_%s", name.toUpperCase());
		}

		@Override
		public void expand(@NonNull final EnvVars env) {
			env.putAll(ibmiEnvVars);
		}

		@Override
		public String toString() {
			return ibmiEnvVars.entrySet()
					.stream()
					.map(envVar -> String.format("%s -> %s", envVar.getKey(), envVar.getValue()))
					.collect(Collectors.joining("\n"));
		}
	}
}
