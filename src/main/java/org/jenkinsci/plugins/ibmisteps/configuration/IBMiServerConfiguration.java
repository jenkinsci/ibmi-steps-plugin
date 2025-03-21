package org.jenkinsci.plugins.ibmisteps.configuration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.ibmisteps.Messages;
import org.jenkinsci.plugins.ibmisteps.model.IBMi;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class IBMiServerConfiguration extends AbstractDescribableImpl<IBMiServerConfiguration> {
	private static final Logger LOGGER = Logger.getLogger(IBMiServerConfiguration.class.getName());

	private final String name;
	private final String host;
	private final String credentialsId;
	private final String ccsid;
	private final boolean secure;

	@DataBoundConstructor
	public IBMiServerConfiguration(final String name, final String host, final String credentialsId, final String ccsid,
			final boolean secure) {
		this.name = name;
		this.host = host;
		this.credentialsId = credentialsId;
		this.ccsid = ccsid;
		this.secure = secure;
	}

	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	public String getCcsid() {
		return ccsid;
	}

	/**
	 * @return <code>0</code> if CCSID is blank, <code>-1</code> if CCSID is not a number, CCSID as an <code>int</code>
	 *         otherwise.
	 */
	public int getCcsidInt() {
		try {
			if (ccsid != null && !ccsid.isBlank()) {
				return Integer.parseInt(ccsid);
			}
			return 0;
		} catch (final NumberFormatException e) {
			return -1;
		}
	}

	public boolean isSecure() {
		return secure;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof final IBMiServerConfiguration otherServer) {
			return name.equals(otherServer.name) &&
					host.equals(otherServer.host) &&
					credentialsId.equals(otherServer.credentialsId) &&
					ccsid.equals(otherServer.ccsid) &&
					secure == otherServer.secure;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, host, credentialsId, ccsid, secure);
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<IBMiServerConfiguration> {
		@RequirePOST
		public final ListBoxModel doFillCredentialsIdItems(@QueryParameter final String value) {
			if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
				return new StandardUsernameListBoxModel().includeCurrentValue(value);
			}
			return new StandardUsernameListBoxModel()
					.includeEmptyValue()
					.includeMatchingAs(
							ACL.SYSTEM2,
							Jenkins.get(),
							StandardUsernamePasswordCredentials.class,
							Collections.emptyList(),
							CredentialsMatchers.always());
		}

		@RequirePOST
		@SuppressWarnings("lgtm[jenkins/no-permission-check]")
		public FormValidation doCheckCredentialsId(@QueryParameter final String credentialsId) {
			if (credentialsId == null || credentialsId.isBlank()) {
				return FormValidation.error(Messages.IBMiServer_credentials_required());
			} else {
				return FormValidation.ok();
			}
		}

		@RequirePOST
		@SuppressWarnings("lgtm[jenkins/no-permission-check]")
		public FormValidation doCheckCcsid(@QueryParameter final String ccsid) {
			if (ccsid != null && !ccsid.isBlank()) {
				try {
					final int ccsidNumber = Integer.parseInt(ccsid);
					if (ccsidNumber < 0 || ccsidNumber > 65535) {
						throw new NumberFormatException();
					} else if (ccsidNumber == 5026) {
						return FormValidation.error(Messages.IBMiServer_ccsid_5026_not_supported());
					}
				} catch (final NumberFormatException e) {
					return FormValidation.error(Messages.IBMiServer_invalid_ccsid());
				}
			}
			return FormValidation.ok();
		}

		@RequirePOST
		public final FormValidation doTestConnection(@QueryParameter(required = true) final String host,
				@QueryParameter(required = true) final String credentialsId,
				@QueryParameter(required = false) final String ccsid,
				@QueryParameter(required = false) final boolean secure,
				@AncestorInPath final Item item) {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			final StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
					CredentialsProvider.lookupCredentialsInItem(
							StandardUsernamePasswordCredentials.class,
							item,
							ACL.SYSTEM2),
					CredentialsMatchers.withId(credentialsId));
			final IBMiServerConfiguration config = new IBMiServerConfiguration("", host, credentialsId, ccsid, secure);
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (PrintStream stream = new PrintStream(output, false, StandardCharsets.UTF_8);
			     IBMi ibmi = new IBMi(stream, host, credentials, config.getCcsidInt(), secure, false)) {
				return FormValidation.ok(Messages.IBMiServer_ConnectionOk(host, ibmi.getOSVersion()));
			} catch (final Exception e) {
				LOGGER.log(Level.SEVERE, Messages.IBMiServer_ConnectionFailed(e.getLocalizedMessage()), e);
				return FormValidation.error(e, Messages.IBMiServer_ConnectionFailed(e.getLocalizedMessage()));
			}
		}

		@Override
		@NonNull
		public String getDisplayName() {
			return "";
		}
	}
}
