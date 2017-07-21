package com.appdynamics.extensions.marklogic;

import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by balakrishnavadavalasa on 03/10/16.
 */
public class MarkLogicMonitorTest {

    @Test
    public void testMarkLogicMonitor() throws TaskExecutionException, IOException {
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "src/test/resources/conf/config.yml");
        taskArgs.put("metrics-file", "src/test/resources/conf/metrics.xml");

        MarkLogicMonitor monitor = new MarkLogicMonitor();
        monitor.execute(taskArgs, null);
    }
}
