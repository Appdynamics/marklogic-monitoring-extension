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

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.marklogic.input.Metric;
import com.appdynamics.extensions.marklogic.input.MetricConfig;
import com.appdynamics.extensions.marklogic.input.MetricGroup;
import com.appdynamics.extensions.marklogic.input.Stat;
import com.appdynamics.extensions.util.JsonUtils;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;


public class MarkLogicMonitorTask implements Runnable {
    private static final Logger logger = Logger.getLogger(MarkLogicMonitorTask.class);

    private Map server;
    private MonitorConfiguration configuration;

    public MarkLogicMonitorTask(MonitorConfiguration configuration, Map server) {
        this.configuration = configuration;
        this.server = server;
    }


    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        String uri = (String) server.get("uri");
        try {
            if (!Strings.isNullOrEmpty(uri)) {
                String displayName = (String) server.get("displayName");
                String serverPrefix;
                if (!Strings.isNullOrEmpty(displayName)) {
                    serverPrefix = configuration.getMetricPrefix() + "|" + displayName + "|";
                } else {
                    serverPrefix = configuration.getMetricPrefix() + "|";
                }
                logger.debug("Fetching metrics for the server uri=" + uri + ",metricPrefix =" + serverPrefix);

                Map<String, Set<String>> entities = new EntityFetcher().fetchEntities(this, configuration, server);
                fetchMetrics(serverPrefix, entities);
            }
        } catch (Exception e) {
            String msg = "Exception while running the MarkLogic task in the server " + uri;
            logger.error(msg, e);
            configuration.getMetricWriter().registerError(msg, e);
        } finally {
            long endTime = System.currentTimeMillis() - startTime;
            logger.debug("MarkLogic monitor thread for server " + uri + " ended. Time taken is " + endTime);
        }
    }

    private void fetchMetrics(String serverPrefix,  Map<String, Set<String>> entities) {
        Stat [] stats = getStats();
        String url;
        if (stats != null && stats.length != 0) {
            for (Stat stat : stats) {
                if(!Strings.isNullOrEmpty(stat.getEntityUrl())) {
                    logger.debug("Started fetching metrics for endpoint " + stat.getEntityUrl());
                    if (!Strings.isNullOrEmpty(stat.getEntity())) {
                        Set<String> entityNames = entities.get(stat.getEntity());
                        for (String entity : entityNames) {
                            url = stat.getEntityUrl().replace("*", entity);
                            fetchMetricsForStat(serverPrefix, stat, url, entity);
                        }
                    } else {
                        url = stat.getEntityUrl();
                        fetchMetricsForStat(serverPrefix, stat, url, null);
                    }
                } else {
                    logger.debug("uri for stat in metrics.xml is not configured");
                }
            }
        } else {
            logger.debug("Stat in metrics.xml is empty");
        }
    }

    private void fetchMetricsForStat(String serverPrefix, Stat stat, String targetUrl, String entity) {
        JsonNode node = getJsonResponseFromTargetUrl( server, targetUrl);
        MetricGroup [] metricGroups = stat.getMetricGroups();
        for (MetricGroup metricGroup : metricGroups) {
            String xpath = metricGroup.getXpath();
            String metricGroupPrefix = metricGroup.getPrefix();
            JsonNode jsonNode = JsonUtils.getNestedObject(node.path(stat.getEntryNode()), xpath.split("\\|"));

            if (jsonNode != null) {
                if (jsonNode.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) jsonNode;
                    for (JsonNode innerNode : arrayNode) {
                        String thisEntityName = entity + "|" + innerNode.path(metricGroup.getInnerNameIdentifier()).asText();
                        printMetricStat(serverPrefix, stat, thisEntityName, metricGroup, metricGroupPrefix, innerNode);
                    }
                } else {
                    printMetricStat(serverPrefix, stat, entity, metricGroup, metricGroupPrefix, jsonNode);
                }
            }
        }
    }

    public JsonNode getJsonResponseFromTargetUrl(Map server, String targetUrl) {
        UrlBuilder urlBuilder = UrlBuilder.fromYmlServerConfig(server).path(targetUrl);
        String url = urlBuilder.build();
        JsonNode node = getResponseAsJson(url);
        return node;
    }

    public JsonNode getResponseAsJson(String targetUrl) {
        return HttpClientUtils.getResponseAsJson(configuration.getHttpClient(), targetUrl, JsonNode.class);
    }

    private void printMetricStat(String serverPrefix, Stat stat, String entity, MetricGroup metricGroup, String metricGroupPrefix, JsonNode jsonNode) {
        Metric[] metrics = metricGroup.getMetrics();
        for (Metric metric : metrics) {
            String metricName = getMetricPath(metricGroupPrefix, metric, jsonNode, entity, stat.getDisplayName());
            printMetric(serverPrefix + metricName, extractMetricValueFromNode(jsonNode, metric.getXpath()), stat);
        }
    }

    private String getMetricPath(String metricGroupPrefix, Metric metric, JsonNode jsonNode, String entityName, String displayName) {
        String metricName = metric.getLabel() + "(" + jsonNode.path(metric.getXpath()).path("units").asText() + ")";
        return MarkLogicUtil.buildMetricNameWithPipes(displayName, entityName, metricGroupPrefix, metricName);
    }


    private Stat[] getStats() {
        MetricConfig statConf = (MetricConfig) configuration.getMetricsXmlConfiguration();
        return statConf.getStats();
    }

    private BigDecimal extractMetricValueFromNode(JsonNode node, String metric) {
        try {
            String value = node.path(metric).path("value").asText();
            String type = node.path(metric).path("units").asText();
            if (type.contentEquals("bool")) {
                if (value.contentEquals("true")) {
                    return new BigDecimal(1);
                } else {
                    return new BigDecimal(0);
                }
            } else {
                return new BigDecimal(value).setScale(0, RoundingMode.HALF_UP);
            }
        } catch (NumberFormatException e) {
            String value = node.path(metric).path("value").asText();
            logger.info(value + " " + value.length());
            logger.info(node.path(metric));
            logger.error("Number exception for metric: " + metric, e);
            return new BigDecimal(0);
        }
    }

    public void printMetric(String metricPath, BigDecimal metricValue, Stat stat) {
        if (metricValue != null) {
            //logger.debug(metricPath + "," + metricValue + "," + getMetricType(stat));
            configuration.getMetricWriter().printMetric(metricPath, metricValue, getMetricType(stat));
        }
    }

    private String getMetricType(Stat stat) {
        if (stat.getMetricType() != null) {
            return stat.getMetricType();
        } else {
            return "AVG.AVG.IND";
        }
    }

    public static void main(String[] args) {

    }
}
