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
package com.appdynamics.extensions.marklogic.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;


@XmlAccessorType(XmlAccessType.FIELD)
public class Stat {
    @XmlElement(name = "metric-group")
    private MetricGroup[] metricGroups;
    @XmlAttribute
    private String entity;
    @XmlAttribute(name="entity-url")
    private String entityUrl;
    @XmlAttribute(name="entry-node")
    private String entryNode;
    @XmlAttribute(name="display-name")
    private String displayName;
    @XmlAttribute(name="metric-type")
    private String metricType;

    public MetricGroup[] getMetricGroups() {
        return metricGroups;
    }

    public void setMetricGroups(MetricGroup[] metricGroups) {
        this.metricGroups = metricGroups;
    }

    public String getEntityUrl() {
        return entityUrl;
    }

    public void setEntityUrl(String entityUrl) {
        this.entityUrl = entityUrl;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getEntryNode() {
        return entryNode;
    }

    public void setEntryNode(String entryNode) {
        this.entryNode = entryNode;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
