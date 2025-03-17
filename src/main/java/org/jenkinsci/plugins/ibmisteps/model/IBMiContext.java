package org.jenkinsci.plugins.ibmisteps.model;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.Serializable;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;

import hudson.model.TaskListener;

public class IBMiContext implements Serializable {
	private static final long serialVersionUID = 7477349510791081645L;
	private final String host;
	private final StandardUsernamePasswordCredentials credentials;
	private final int ccsid;
	private final boolean secure;
	private final String iasp;
	private final boolean traceEnabled;
	private transient IBMi ibmi;

	public IBMiContext(final String host,
			final StandardUsernamePasswordCredentials credentials,
			final int ccsid,
			final boolean secure,
			final String iasp,
			final boolean traceEnabled) {
		this.host = host;
		this.credentials = credentials;
		this.ccsid = ccsid;
		this.secure = secure;
		this.iasp = iasp;
		this.traceEnabled = traceEnabled;
	}

	public IBMi getIBMi(final TaskListener listener) throws IOException, InterruptedException, PropertyVetoException,
			AS400SecurityException, ErrorCompletingRequestException {
		if (ibmi == null) {
			ibmi = new IBMi(listener.getLogger(),
					host,
					credentials,
					ccsid,
					secure,
					traceEnabled);
			ibmi.changeIASP(iasp);
			ibmi.onDisconnected(e -> ibmi = null);
		}
		return ibmi;
	}

	public boolean isTraceEnabled() {
		return traceEnabled;
	}

	public void close() {
		if (ibmi != null) {
			ibmi.disconnect();
		}
	}
}
