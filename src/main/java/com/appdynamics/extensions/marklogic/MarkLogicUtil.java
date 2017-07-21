package com.appdynamics.extensions.marklogic;

import com.google.common.base.Strings;

/**
 * Created by balakrishnavadavalasa on 13/07/17.
 */
public class MarkLogicUtil {

    public static String buildMetricNameWithPipes(String... metricSegments) {
        StringBuilder sb = new StringBuilder();
        for (String metricSegment : metricSegments) {
            if (!Strings.isNullOrEmpty(metricSegment)) {
                sb.append(metricSegment).append("|");
            }
        }
        String finalMetricName = sb.toString();
        return finalMetricName.substring(0, finalMetricName.length() - 1);
    }

}
