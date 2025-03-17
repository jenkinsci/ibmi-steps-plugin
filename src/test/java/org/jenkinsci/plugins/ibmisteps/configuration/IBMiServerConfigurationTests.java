package org.jenkinsci.plugins.ibmisteps.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.jenkinsci.plugins.ibmisteps.configuration.IBMiServerConfiguration.DescriptorImpl;
import org.junit.Test;

import hudson.util.FormValidation;

public class IBMiServerConfigurationTests {

	@Test
	public void testChecks() {
		final DescriptorImpl serverDescriptor = new IBMiServerConfiguration.DescriptorImpl();

		final IBMiServerConfiguration validServer = new IBMiServerConfiguration("An IBMi server", "server.address",
				"1234567890", "0", false);
		assertEquals("CCSID must be a number", FormValidation.Kind.OK,
				serverDescriptor.doCheckCcsid(validServer.getCcsid()).kind);
		assertEquals("credentials are mandatory", FormValidation.Kind.OK,
				serverDescriptor.doCheckCredentialsId(validServer.getCredentialsId()).kind);

		final IBMiServerConfiguration invalidServer = new IBMiServerConfiguration("", "", "", "NaN", false);
		assertEquals("CCSID must be a number", FormValidation.Kind.ERROR,
				serverDescriptor.doCheckCcsid(invalidServer.getCcsid()).kind);
		assertEquals("credentials are mandatory", FormValidation.Kind.ERROR,
				serverDescriptor.doCheckCredentialsId(invalidServer.getCredentialsId()).kind);

		assertEquals("CCSID must be a number", FormValidation.Kind.ERROR,
				serverDescriptor.doCheckCcsid("X").kind);
		assertEquals("CCSID must be positive", FormValidation.Kind.ERROR,
				serverDescriptor.doCheckCcsid("-1").kind);
		assertEquals("CCSID must be < 65535", FormValidation.Kind.ERROR,
				serverDescriptor.doCheckCcsid("666666").kind);
		assertEquals("CCSID cannot be 5026", FormValidation.Kind.ERROR,
				serverDescriptor.doCheckCcsid("5026").kind);
	}

	@Test
	public void testEquals() {
		final IBMiServerConfiguration server = new IBMiServerConfiguration("An IBMi server", "server.address",
				"1234567890", "37", true);
		final IBMiServerConfiguration serverEquals = new IBMiServerConfiguration("An IBMi server", "server.address",
				"1234567890", "37", true);
		final IBMiServerConfiguration serverNotEquals1 = new IBMiServerConfiguration("Another IBMi server",
				"server.address", "1234567890", "37",
				true);
		final IBMiServerConfiguration serverNotEquals2 = new IBMiServerConfiguration("An IBMi server",
				"server.address.else", "1234567890", "37",
				true);
		final IBMiServerConfiguration serverNotEquals3 = new IBMiServerConfiguration("An IBMi server", "server.address",
				"a6d5ecee", "37", true);
		final IBMiServerConfiguration serverNotEquals4 = new IBMiServerConfiguration("An IBMi server", "server.address",
				"1234567890", "5026",
				true);
		final IBMiServerConfiguration serverNotEquals5 = new IBMiServerConfiguration("An IBMi server", "server.address",
				"1234567890", "37",
				false);

		assertEquals("Servers are equal", server, serverEquals);
		assertNotEquals("name is different", server, serverNotEquals1);
		assertNotEquals("host is different", server, serverNotEquals2);
		assertNotEquals("credentials are different", server, serverNotEquals3);
		assertNotEquals("CCSID is different", server, serverNotEquals4);
		assertNotEquals("secure is different", server, serverNotEquals5);
	}

	@Test
	public void testCCSIDConversion() {
		assertEquals(37, new IBMiServerConfiguration("", "", "", "37", false).getCcsidInt());
		assertEquals(0, new IBMiServerConfiguration("", "", "", "0", false).getCcsidInt());
		assertEquals(0, new IBMiServerConfiguration("", "", "", "", false).getCcsidInt());
		assertEquals(-1, new IBMiServerConfiguration("", "", "", "nope", false).getCcsidInt());
	}
}
