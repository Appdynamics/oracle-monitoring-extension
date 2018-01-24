package com.appdynamics.extensions.oracledb;

import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bhuvnesh.kumar on 1/23/18.
 */
public class OracledbMonitorTest {

    private static final String CONFIG_ARG = "config-file";

    private OracledbMonitor testClass;

    @Before
    public void init() throws Exception {

        testClass = new OracledbMonitor();
    }


    @Test
    public void testSQLMonitoringExtension() throws TaskExecutionException {
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/Users/bhuvnesh.kumar/repos/appdynamics/extensions/vertica-monitoring-extension/src/test/resources/conf/config_generic.yml");
        testClass.execute(taskArgs, null);

    }

}
