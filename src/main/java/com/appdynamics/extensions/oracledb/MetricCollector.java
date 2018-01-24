package com.appdynamics.extensions.oracledb;

import com.appdynamics.extensions.metrics.Metric;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bhuvnesh.kumar on 9/29/17.
 */
public class MetricCollector {
    private static final String METRIC_SEPARATOR = "|";
    private String metricPrefix;
    private String dbServerDisplayName;
    private String queryDisplayName;
    private List<Map<String, String>> metricReplacer;


    public MetricCollector(String metricPrefix, String dbServerDisplayName, String queryDisplayName, List<Map<String, String>> metricReplacer) {
        this.metricPrefix = metricPrefix;
        this.dbServerDisplayName = dbServerDisplayName;
        this.queryDisplayName = queryDisplayName;
        this.metricReplacer = metricReplacer;
    }

    public List<Metric> goingThroughResultSet(ResultSet resultSet, List<Column> columns) throws SQLException {
        List<Metric> list_of_metrics = new ArrayList<Metric>();
        while (resultSet != null && resultSet.next()) {
            String metricPath = "";
            boolean metricPathAlreadyAdded = false;
            metricPath = getMetricPrefix(dbServerDisplayName, queryDisplayName);
            for (Column c : columns) {
                if (c.getType().equals("metricPathName")) {
                    if (metricPathAlreadyAdded == false) {
                        metricPath += METRIC_SEPARATOR + resultSet.getString(c.getName());
                        metricPathAlreadyAdded = true;
                    } else {
                        metricPath += METRIC_SEPARATOR + resultSet.getString(c.getName());
                    }
                } else if (c.getType().equals("metricValue")) {
                    String updatedMetricPath = metricPath + METRIC_SEPARATOR + c.getName();
                    String val = resultSet.getString(c.getName());

                    if (val != null) {
                        val = replaceCharacter(val);
                        updatedMetricPath = replaceCharacter(updatedMetricPath);
                        Metric current_metric;
                        if (c.getProperties() != null) {
                            current_metric = new Metric(c.getName(), val, updatedMetricPath, c.getProperties());
                        } else {
                            current_metric = new Metric(c.getName(), val, updatedMetricPath);
                        }
                        list_of_metrics.add(current_metric);

                    }
                }
            }
        }
        print(list_of_metrics);

        return list_of_metrics;
    }

    private void print(List<Metric> metrics){

        for(Metric metric: metrics){
            System.out.println(metric.getMetricPath() + " :: " + metric.getMetricValue());
        }
    }


    private String replaceCharacter(String metricPath) {

        for (Map chars : metricReplacer) {
            String replace = (String) chars.get("replace");
            String replaceWith = (String) chars.get("replaceWith");

            if (metricPath.contains(replace)) {
                metricPath = metricPath.replaceAll(replace, replaceWith);
            }
        }
        return metricPath;
    }


    private String getMetricPrefix(String dbServerDisplayName, String queryDisplayName) {
        return metricPrefix + METRIC_SEPARATOR + dbServerDisplayName + METRIC_SEPARATOR + queryDisplayName;
    }

}
