package org.jenkinsci.plugins.ibmisteps.configuration;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.ibmisteps.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.ExtensionList;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@Extension
public class IBMiGlobalConfiguration extends GlobalConfiguration {
	private static final Logger LOGGER = Logger.getLogger(IBMiGlobalConfiguration.class.getName());

	private List<IBMiServerConfiguration> servers = Collections.emptyList();

	@DataBoundConstructor
	public IBMiGlobalConfiguration() {
		load();
	}

	public static IBMiGlobalConfiguration get() {
		return ExtensionList.lookupSingleton(IBMiGlobalConfiguration.class);
	}

	@CheckForNull
	public IBMiServerConfiguration getServer(final String name) {
		return servers.stream()
				.filter(server -> server.getName().equals(name))
				.findFirst()
				.orElse(null);
	}

	public List<IBMiServerConfiguration> getServers() {
		return servers;
	}

	public void setServers(final List<IBMiServerConfiguration> servers) {
		this.servers = servers;
		save();
	}

	@Override
	public boolean configure(final StaplerRequest2 req, final JSONObject json) throws FormException {
		servers.clear();
		final boolean configured = super.configure(req, json);
		for (final IBMiServerConfiguration server : servers) {
			if (servers.stream().anyMatch(s -> s != server && s.getName().equals(server.getName()))
					&& LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.warning(Messages.IBMiGlobalConfiguration_duplicated_servers(server.getName()));
			}
		}
		return configured;
	}
}
