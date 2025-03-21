package org.jenkinsci.plugins.ibmisteps.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import hudson.util.FormValidation;
import org.jenkinsci.plugins.ibmisteps.configuration.IBMiServerConfiguration.DescriptorImpl;
import org.junit.jupiter.api.Test;

class IBMiServerConfigurationTests {

    @Test
    void testChecks() {
        final DescriptorImpl serverDescriptor = new IBMiServerConfiguration.DescriptorImpl();

        final IBMiServerConfiguration validServer =
                new IBMiServerConfiguration("An IBMi server", "server.address", "1234567890", "0", false);
        assertEquals(
                FormValidation.Kind.OK,
                serverDescriptor.doCheckCcsid(validServer.getCcsid()).kind,
                "CCSID must be a number");
        assertEquals(
                FormValidation.Kind.OK,
                serverDescriptor.doCheckCredentialsId(validServer.getCredentialsId()).kind,
                "credentials are mandatory");

        final IBMiServerConfiguration invalidServer = new IBMiServerConfiguration("", "", "", "NaN", false);
        assertEquals(
                FormValidation.Kind.ERROR,
                serverDescriptor.doCheckCcsid(invalidServer.getCcsid()).kind,
                "CCSID must be a number");
        assertEquals(
                FormValidation.Kind.ERROR,
                serverDescriptor.doCheckCredentialsId(invalidServer.getCredentialsId()).kind,
                "credentials are mandatory");

        assertEquals(FormValidation.Kind.ERROR, serverDescriptor.doCheckCcsid("X").kind, "CCSID must be a number");
        assertEquals(FormValidation.Kind.ERROR, serverDescriptor.doCheckCcsid("-1").kind, "CCSID must be positive");
        assertEquals(FormValidation.Kind.ERROR, serverDescriptor.doCheckCcsid("666666").kind, "CCSID must be < 65535");
        assertEquals(FormValidation.Kind.ERROR, serverDescriptor.doCheckCcsid("5026").kind, "CCSID cannot be 5026");
    }

    @Test
    void testEquals() {
        final IBMiServerConfiguration server =
                new IBMiServerConfiguration("An IBMi server", "server.address", "1234567890", "37", true);
        final IBMiServerConfiguration serverEquals =
                new IBMiServerConfiguration("An IBMi server", "server.address", "1234567890", "37", true);
        final IBMiServerConfiguration serverNotEquals1 =
                new IBMiServerConfiguration("Another IBMi server", "server.address", "1234567890", "37", true);
        final IBMiServerConfiguration serverNotEquals2 =
                new IBMiServerConfiguration("An IBMi server", "server.address.else", "1234567890", "37", true);
        final IBMiServerConfiguration serverNotEquals3 =
                new IBMiServerConfiguration("An IBMi server", "server.address", "a6d5ecee", "37", true);
        final IBMiServerConfiguration serverNotEquals4 =
                new IBMiServerConfiguration("An IBMi server", "server.address", "1234567890", "5026", true);
        final IBMiServerConfiguration serverNotEquals5 =
                new IBMiServerConfiguration("An IBMi server", "server.address", "1234567890", "37", false);

        assertEquals(server, serverEquals, "Servers are equal");
        assertNotEquals(server, serverNotEquals1, "name is different");
        assertNotEquals(server, serverNotEquals2, "host is different");
        assertNotEquals(server, serverNotEquals3, "credentials are different");
        assertNotEquals(server, serverNotEquals4, "CCSID is different");
        assertNotEquals(server, serverNotEquals5, "secure is different");
    }

    @Test
    void testCCSIDConversion() {
        assertEquals(37, new IBMiServerConfiguration("", "", "", "37", false).getCcsidInt());
        assertEquals(0, new IBMiServerConfiguration("", "", "", "0", false).getCcsidInt());
        assertEquals(0, new IBMiServerConfiguration("", "", "", "", false).getCcsidInt());
        assertEquals(-1, new IBMiServerConfiguration("", "", "", "nope", false).getCcsidInt());
    }
}
