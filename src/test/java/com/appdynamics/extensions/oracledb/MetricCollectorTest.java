/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.oracledb;

import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by bhuvnesh.kumar on 10/5/17.
 */
public class MetricCollectorTest {
    private static final String METRIC_SEPARATOR = "|";
    private String metricPrefix = "metricPrefix";
    private String dbServerDisplayName = "dbServer";

    private String queryDisplayName = "queryName";
    private List<Map<String, String>> metricReplacer = getMetricReplacer();


    @Test
    public void testGoingThroughResultSetWithNormalValues() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        List<Metric> list_of_metrics;
        when(resultSet.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);

        String num1 = "6";
        String num2 = "7";

        when(resultSet.getString("NODE_NAME")).thenReturn("metricPathName");
        when(resultSet.getString("AVERAGE_MEMORY_USAGE_PERCENT")).thenReturn(num1);
        when(resultSet.getString("AVERAGE_CPU_USAGE_PERCENT")).thenReturn(num2);

        Map queries = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/config_for_columns.yml"));
        ColumnGenerator columnGenerator = new ColumnGenerator();
        List<Column> columns = columnGenerator.getColumns(queries);

        MetricCollector metricCollector = new MetricCollector(metricPrefix, dbServerDisplayName, queryDisplayName, metricReplacer);

        list_of_metrics = metricCollector.goingThroughResultSet(resultSet, columns);

        for (Metric listMetric : list_of_metrics) {
            Boolean check = false;
            for (Column column : columns) {
                String name = column.getName();
                if (name == listMetric.getMetricName()) {
                    check = true;
                }
            }

            Assert.assertTrue(check);
        }
        Assert.assertTrue(list_of_metrics.size() == 2);

    }

    public List<Map<String, String>> getMetricReplacer() {

        List<Map<String, String>> replacerList = new ArrayList<Map<String, String>>();
        Map<String, String> replaceThis1 = new HashMap<String, String>();
        Map<String, String> replaceThis2 = new HashMap<String, String>();

        replaceThis1.put("replace", "%");
        replaceThis1.put("replaceWith", "");

        replaceThis2.put("replace", ",");
        replaceThis2.put("replaceWith", "-");

        replacerList.add(replaceThis1);
        replacerList.add(replaceThis2);

        return replacerList;

    }

    @Test
    public void testGoingThroughResultSetWithConvertMap() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        List<Metric> list_of_metrics;
        when(resultSet.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);


        String num1 = "DOWN";
        when(resultSet.getString("NODE_NAME")).thenReturn("metricPathName");
        when(resultSet.getString("NODE_STATE")).thenReturn(num1);


        Map queries = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/config_convert.yml"));
        ColumnGenerator columnGenerator = new ColumnGenerator();
        List<Column> columns = columnGenerator.getColumns(queries);

        MetricCollector metricCollector = new MetricCollector(metricPrefix, dbServerDisplayName, queryDisplayName, metricReplacer);

        list_of_metrics = metricCollector.goingThroughResultSet(resultSet, columns);

        for (Metric listMetric : list_of_metrics) {
            Boolean check = false;
            for (Column column : columns) {
                String name = column.getName();
                if (name == listMetric.getMetricName()) {
                    check = true;
                }
            }

            Assert.assertTrue(listMetric.getMetricValue() == num1);

            Assert.assertTrue(check);
        }
        Assert.assertTrue(list_of_metrics.size() == 1);

    }

    @Test
    public void testingForCommaAndPercentSignRemoval() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        List<Metric> list_of_metrics;
        when(resultSet.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);

        String num1 = "6%";
        String num2 = "7";

        when(resultSet.getString("NODE_NAME")).thenReturn("metricPath,Name");
        when(resultSet.getString("AVERAGE_MEMORY_USAGE_PERCENT")).thenReturn(num1);
        when(resultSet.getString("AVERAGE_CPU_USAGE_PERCENT")).thenReturn(num2);

        Map queries = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/config_for_columns.yml"));
        ColumnGenerator columnGenerator = new ColumnGenerator();
        List<Column> columns = columnGenerator.getColumns(queries);

        MetricCollector metricCollector = new MetricCollector(metricPrefix, dbServerDisplayName, queryDisplayName, metricReplacer);

        list_of_metrics = metricCollector.goingThroughResultSet(resultSet, columns);

        for (Metric listMetric : list_of_metrics) {

            Assert.assertFalse(listMetric.getMetricPath().contains(","));
            Assert.assertFalse(listMetric.getMetricValue().contains("%"));
        }
        Assert.assertTrue(list_of_metrics.size() == 2);

    }


}
