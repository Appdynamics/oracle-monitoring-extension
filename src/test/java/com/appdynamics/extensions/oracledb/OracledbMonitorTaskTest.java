/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.oracledb;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by bhuvnesh.kumar on 1/23/18.
 */
public class OracledbMonitorTaskTest {
    private long previousTimestamp = System.currentTimeMillis();
    private long currentTimestamp = System.currentTimeMillis();
    private String metricPrefix = "Custom Metrics";
    private MetricWriteHelper metricWriter = mock(MetricWriteHelper.class);
    JDBCConnectionAdapter jdbcAdapter = mock(JDBCConnectionAdapter.class);
    private Map server;


    @Test
    public void testRunFunctionality() throws SQLException, ClassNotFoundException {
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

        Map servers_yaml = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/config1.yml"));
        List<Map<String, String>> servers = (List<Map<String, String>>) servers_yaml.get("dbServers");

        server = servers.get(0);
        currentTimestamp = System.currentTimeMillis();
        Connection connection = mock(Connection.class);
        when(jdbcAdapter.open((String) server.get("driver"))).thenReturn(connection);

        OracledbMonitorTask sqlMonitorTask = new OracledbMonitorTask.Builder().metricWriter(metricWriter)
                .metricPrefix(metricPrefix)
                .jdbcAdapter(jdbcAdapter)
                .previousTimestamp(previousTimestamp)
                .currentTimestamp(currentTimestamp)
                .server(server).build();
        Statement statement = connection.createStatement();

        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);

        when(resultSet.getString("NODE_NAME")).thenReturn("v_vmart_node0001");
        when(resultSet.getString("EVENT_ID")).thenReturn("6");
        when(resultSet.getString("EVENT_CODE")).thenReturn("6");
        when(resultSet.getString("EVENT_POSTED_COUNT")).thenReturn("1");

        when(jdbcAdapter.queryDatabase("Select NODE_NAME, EVENT_CODE, EVENT_ID, EVENT_POSTED_COUNT from Active_events", statement)).thenReturn(resultSet);

        sqlMonitorTask.run();
        verify(metricWriter).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Custom Metrics|Vertica|Active Events|v_vmart_node0001|6|EVENT_CODE");
        metricPathsList.add("Custom Metrics|Vertica|Active Events|v_vmart_node0001|6|EVENT_POSTED_COUNT");

        for (Metric metric : (List<Metric>) pathCaptor.getValue()) {
            Assert.assertTrue(metricPathsList.contains(metric.getMetricPath()));
        }

    }

}
