package org.jenkinsci.plugins.ibmisteps.configuration;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IBMiGlobalConfigurationTests {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    @LocalData
    public void loadConfigurationTest() {
        final IBMiGlobalConfiguration configuration = IBMiGlobalConfiguration.get();
        assertNotNull(configuration);

        final IBMiServerConfiguration server1 = configuration.getServer("IBMi 1");
        assertNotNull("Server IBMi 1 must exist", server1);
        assertEquals(new IBMiServerConfiguration("IBMi 1",
                        "ibmi1.sebjulliand.github.com",
                        "a6d5ecee-c167-4ec5-8b5e-ee39ddb5c96e",
                        "",
                        true),
                server1);

        final IBMiServerConfiguration server2 = configuration.getServer("IBMi 2");
        assertNotNull("Server IBMi 2 must exist", server2);
        assertEquals(new IBMiServerConfiguration("IBMi 2",
                        "ibmi2.sebjulliand.github.com",
                        "hello-world",
                        "37",
                        false),
                server2);

        final IBMiServerConfiguration notFoundServer = configuration.getServer("NotFound");
        assertNull("Server 'NotFound' doesn't exist", notFoundServer);
    }
}
