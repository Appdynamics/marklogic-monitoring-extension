/**
 * Copyright 2017 AppDynamics
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.marklogic;

import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.conf.MonitorConfiguration.ConfItem;
import com.appdynamics.extensions.marklogic.input.MetricConfig;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MarkLogicMonitor extends AManagedMonitor {

    private static final Logger logger = Logger.getLogger(MarkLogicMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|MarkLogic|";
    private static final String METRIC_XML_ARG = "metrics-file";

    private boolean initialized;
    private MonitorConfiguration configuration;

    public MarkLogicMonitor() {
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logVersion();

        if (!initialized) {
            initialize(taskArgs);
        }
        logger.debug("The raw arguments are " + taskArgs);
        configuration.executeTask();
        logger.info("MarkLogic monitor run completed successfully.");
        return new TaskOutput("MarkLogic monitor run completed successfully.");
    }

    private void initialize(Map<String, String> taskArgs) {
        if(!initialized) {
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            String metricXMLFilePath = getPath(taskArgs, METRIC_XML_ARG, "monitors/MarkLogicMonitor/metrics.xml");
            conf.setMetricsXml(metricXMLFilePath, MetricConfig.class);
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(ConfItem.CONFIG_YML, ConfItem.METRICS_XML, ConfItem.EXECUTOR_SERVICE, ConfItem.HTTP_CLIENT, ConfItem.METRIC_PREFIX, ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            if(config != null) {
                List<Map> servers = (List) config.get("servers");
                if(servers != null && !servers.isEmpty()) {
                    for(Map server : servers) {
                        MarkLogicMonitorTask task = new MarkLogicMonitorTask(configuration, server);
                        configuration.getExecutorService().execute(task);
                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                if (config == null) {
                    logger.error("The config.yml is not loaded due to previous errors.The task will not run");
                }
            }

        }
    }

    private String getPath(Map<String, String> taskArgs, String pathKey, String defaultValue) {
        String path = taskArgs.get(pathKey);
        if (StringUtils.hasText(path)) {
            return path.trim();
        } else {
            return defaultValue;
        }
    }

    private static String getImplementationVersion() {
        return MarkLogicMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.TRACE);

        logger.getRootLogger().addAppender(ca);

        final MarkLogicMonitor monitor = new MarkLogicMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "src/test/resources/conf/config.yml");
        taskArgs.put("metrics-file", "src/test/resources/conf/test-metrics.xml");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 30, TimeUnit.SECONDS);
    }
}
