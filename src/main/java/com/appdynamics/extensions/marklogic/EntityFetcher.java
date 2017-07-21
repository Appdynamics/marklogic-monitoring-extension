package com.appdynamics.extensions.marklogic;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.marklogic.input.EntityList;
import com.appdynamics.extensions.marklogic.input.MetricConfig;
import com.appdynamics.extensions.util.JsonUtils;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntityFetcher {
    private static final Logger logger = Logger.getLogger(MarkLogicMonitorTask.class);

    public Map<String, Set<String>> fetchEntities(MarkLogicMonitorTask markLogicMonitorTask, MonitorConfiguration configuration, Map server) {
        MetricConfig metricConfig = (MetricConfig) configuration.getMetricsXmlConfiguration();
        EntityList[] entityListsXML = metricConfig.getEntityList();
        Map<String, Set<String>> entities = Maps.newHashMap();
        if (entityListsXML != null & entityListsXML.length != 0) {
            for (EntityList entityListXml : entityListsXML) {
                String entityName = entityListXml.getName();
                String uri = entityListXml.getUrl();
                logger.debug("Started fetching " + entityName + "from endpoint " + uri);
                JsonNode node = markLogicMonitorTask.getJsonResponseFromTargetUrl(server, uri);
                JsonNode entityListNode = JsonUtils.getNestedObject(node, entityListXml.getXpath().split("\\|"));
                Set<String> entityNames = new HashSet<String>();
                if (entityListNode.isArray()) {

                    ArrayNode nodes = (ArrayNode) entityListNode;
                    for (JsonNode list : nodes) {
                        String name = list.path(entityListXml.getNameIdentifier()).asText();
                        if (isMatchingEntity(name, entityListXml.getFilter().getInclude())) {
                            entityNames.add(name);
                        }
                    }
                }
                entities.put(entityName, entityNames);
            }
        }
        return entities;
    }

    private boolean isMatchingEntity(String entityName, String include) {
        if ("*".equals(include)) {
            return true;
        }
        String [] includes = include.split(",");

        for (final String regex : includes) {
            if (entityName.matches(regex)) {
                return true;
            }
        }
        return false;
    }
}