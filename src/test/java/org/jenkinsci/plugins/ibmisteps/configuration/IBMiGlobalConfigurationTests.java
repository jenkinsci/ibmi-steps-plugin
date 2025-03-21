package org.jenkinsci.plugins.ibmisteps.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

@WithJenkins
class IBMiGlobalConfigurationTests {

    @Test
    @LocalData
    void loadConfigurationTest(JenkinsRule jenkins) {
        final IBMiGlobalConfiguration configuration = IBMiGlobalConfiguration.get();
        assertNotNull(configuration);

        final IBMiServerConfiguration server1 = configuration.getServer("IBMi 1");
        assertNotNull(server1, "Server IBMi 1 must exist");
        assertEquals(
                new IBMiServerConfiguration(
                        "IBMi 1", "ibmi1.sebjulliand.github.com", "a6d5ecee-c167-4ec5-8b5e-ee39ddb5c96e", "", true),
                server1);

        final IBMiServerConfiguration server2 = configuration.getServer("IBMi 2");
        assertNotNull(server2, "Server IBMi 2 must exist");
        assertEquals(
                new IBMiServerConfiguration("IBMi 2", "ibmi2.sebjulliand.github.com", "hello-world", "37", false),
                server2);

        final IBMiServerConfiguration notFoundServer = configuration.getServer("NotFound");
        assertNull(notFoundServer, "Server 'NotFound' doesn't exist");
    }
}
