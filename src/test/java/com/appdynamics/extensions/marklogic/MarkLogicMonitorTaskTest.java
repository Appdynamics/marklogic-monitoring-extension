package com.appdynamics.extensions.marklogic;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.marklogic.input.MetricConfig;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MarkLogicMonitorTaskTest extends AManagedMonitor {

    public static final Logger logger = LoggerFactory.getLogger(MarkLogicMonitorTask.class);

    private Map server;
    private MetricWriteHelper writer;
    private MonitorConfiguration configuration;
    private MarkLogicMonitorTask markLogicMonitorTask;


    @Before
    public void setup() {
        Runnable runnable = Mockito.mock(Runnable.class);
        writer = MetricWriteHelperFactory.create(this);
        writer = Mockito.spy(writer);
        configuration = new MonitorConfiguration("Test", runnable, writer);
        configuration = Mockito.spy(configuration);
        configuration.setConfigYml("src/test/resources/conf/config.yml");
        server = ((List<Map<String, ?>>) configuration.getConfigYml().get("servers")).get(0);
        markLogicMonitorTask = Mockito.spy(new MarkLogicMonitorTask(configuration, server));
    }

    @Test
    public void testEntityCount() throws IOException {
        configuration.setMetricsXml("src/test/resources/conf/test-metrics.xml", MetricConfig.class);
        MarkLogicMonitorTask task = getResourceFromUrl(configuration, server);
        EntityFetcher entityFetcher = Mockito.spy(EntityFetcher.class);
        Map<String, Set<String>> entities = entityFetcher.fetchEntities(task, configuration, server);
        Assert.assertEquals(1, entities.get("Servers").size());
    }

    @Test
    public void testMarkLogicMonitorTask() throws IOException {
        final List<MetricOutput> expected = MetricOutput.from("/data/expectedoutput.txt");
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] arguments = invocationOnMock.getArguments();
                MetricOutput output = new MetricOutput((String) arguments[0], (BigDecimal) arguments[1], (String) arguments[2]);
                boolean remove = expected.remove(output);
                if (!remove) {
                    logger.error("Cannot find the value in the expected values {}", output);
                } else{
                    logger.debug("Received an expected metric "+output);
                }
                return null;
            }
        }).when(writer).printMetric(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.anyString());

        configuration.setMetricsXml("src/test/resources/conf/test-metrics.xml", MetricConfig.class);
        MarkLogicMonitorTask task = getResourceFromUrl(configuration, server);
        task.run();
        System.out.println(expected.size());
        Assert.assertTrue("It seems that these metrics are not reported " + expected, expected.isEmpty());
    }

    private MarkLogicMonitorTask getResourceFromUrl(MonitorConfiguration configuration, Map server) {
        MarkLogicMonitorTask task = new MarkLogicMonitorTask(configuration, server);
        task = Mockito.spy(task);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String url = (String) invocationOnMock.getArguments()[0];
                if (url.endsWith("manage/v2/servers?format=json")) {
                    return getResourceAsJson("src/test/resources/data/servers-list.json");
                } else if (url.endsWith("manage/v2/databases?format=json")) {
                    return getResourceAsJson("src/test/resources/data/databases-list.json");
                } else if (url.endsWith("manage/v2/forests?format=json")) {
                    return getResourceAsJson("src/test/resources/data/forests-list.json");

                } else if (url.endsWith("/manage/v2?view=status&format=json")) {
                    return getResourceAsJson("src/test/resources/data/local-cluster-status.json");
                } else if (url.endsWith("/manage/v2/databases/Documents?view=status&format=json")) {
                    return getResourceAsJson("src/test/resources/data/database-status.json");
                } else if (url.endsWith("/manage/v2/forests/Meters?view=status&format=json")) {
                    return getResourceAsJson("src/test/resources/data/forest-status.json");
                } else if (url.endsWith("/manage/v2/servers/Admin?view=status&group-id=Default&format=json")) {
                    return getResourceAsJson("src/test/resources/data/server-status.json");
                } else {
                    return null;
                }
            }
        }).when(task).getResponseAsJson(Mockito.anyString());
        return task;
    }

    private JsonNode getResourceAsJson(String path) throws IOException {
        return new ObjectMapper().readTree(new File(path));
    }

    @Override
    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        return null;
    }
}
