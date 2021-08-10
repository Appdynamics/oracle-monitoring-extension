/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.oracledb;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.ConsoleAppender;

import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bhuvnesh.kumar on 1/23/18.
 */
public class OracledbMonitorTask implements AMonitorTaskRunnable {

    private long previousTimestamp;
    private long currentTimestamp;
    private String metricPrefix;
    private MetricWriteHelper metricWriter;
    private JDBCConnectionAdapter jdbcAdapter;
    private Map server;
    private Boolean status = true;
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(OracledbMonitor.class);

    public void run() {
        List<Map> queries = (List<Map>) server.get("queries");
        Connection connection = null;
        if (queries != null && !queries.isEmpty()) {
            try {
                long timeBeforeConnection = System.currentTimeMillis();
                connection = getConnection();
                long timeAfterConnection = System.currentTimeMillis();
                String dbServerDisplayName = (String) server.get("displayName");
                logger.debug("Time taken to get Connection for " + dbServerDisplayName + " : " + (timeAfterConnection - timeBeforeConnection));

                if(connection != null) {
                    logger.debug(" Connection successful for server: " + dbServerDisplayName);

                    for (Map query : queries) {
                        try {
                            executeQuery(connection, query);
                        } catch (SQLException e) {
                            logger.error("Error during executing query.");
                        }
                    }
                } else {

                    logger.debug("Null Connection returned for server: " + dbServerDisplayName);
                }

            } catch (SQLException e) {
                logger.error("Error Opening connection", e);
                status = false;
            } catch (ClassNotFoundException e) {
                logger.error("Class not found while opening connection", e);
                status = false;
            } finally {
                try {
                    if (connection != null) {
                        closeConnection(connection);
                    }
                } catch (Exception e) {
                    logger.error("Issue closing the connection", e);
                }
            }
        }
    }

    private void executeQuery(Connection connection, Map query) throws SQLException {
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = getStatement(connection);
            resultSet = getResultSet(query, statement);
            getMetricsFromResultSet(query, resultSet);

        } catch (SQLException e) {
            logger.error("Error in connecting the result. ", e);
        } finally {

            if (statement != null) try {
                jdbcAdapter.closeStatement(statement);
            } catch (SQLException e) {
                logger.error("Unable to close the Statement", e);
            }

            if (resultSet != null) try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Unable to close the ResultSet", e);
            }
        }
    }

    private void getMetricsFromResultSet(Map query, ResultSet resultSet) throws SQLException {
        String dbServerDisplayName = (String) server.get("displayName");
        String queryDisplayName = (String) query.get("displayName");

        logger.debug("Received ResultSet and now extracting metrics for query {}", queryDisplayName);

        ColumnGenerator columnGenerator = new ColumnGenerator();
        List<Column> columns = columnGenerator.getColumns(query);
        List<Map<String, String>> metricReplacer = getMetricReplacer();

        MetricCollector metricCollector = new MetricCollector(metricPrefix, dbServerDisplayName, queryDisplayName, metricReplacer);

        Map<String, Metric> metricMap = metricCollector.goingThroughResultSet(resultSet, columns);
        List<Metric> metricList = getListMetrics(metricMap);

        metricWriter.transformAndPrintMetrics(metricList);

    }

    private List<Metric> getListMetrics(Map<String, Metric> metricMap) {
        List<Metric> metricList = new ArrayList<Metric>();
        for (String path : metricMap.keySet()) {
            metricList.add(metricMap.get(path));
        }
        return metricList;

    }

    private Statement getStatement(Connection connection) throws SQLException {
        return connection.createStatement();
    }

    private ResultSet getResultSet(Map query, Statement statement) throws SQLException {
        String queryStmt = (String) query.get("queryStmt");
        queryStmt = substitute(queryStmt);
        long timeBeforeQuery = System.currentTimeMillis();
        ResultSet resultSet = jdbcAdapter.queryDatabase(queryStmt, statement);
        long timeAfterQuery = System.currentTimeMillis();

        logger.debug("Queried the database in :" + (timeAfterQuery - timeBeforeQuery) + " ms for query: \n " + queryStmt);

        return resultSet;
    }


    private List<Map<String, String>> getMetricReplacer() {
        List<Map<String, String>> metricReplace = (List<Map<String, String>>) server.get("metricCharacterReplacer");
        return metricReplace;
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Connection connection = jdbcAdapter.open((String) server.get("driver"));
        return connection;
    }

    private void closeConnection(Connection connection) throws Exception {
        jdbcAdapter.closeConnection(connection);
    }

    private String substitute(String statement) {
        String stmt = statement;
        stmt = stmt.replace("{{previousTimestamp}}", Long.toString(previousTimestamp));
        stmt = stmt.replace("{{currentTimestamp}}", Long.toString(currentTimestamp));
        return stmt;
    }

    public void onTaskComplete() {
        logger.debug("Task Complete");
        if (status == true) {
            metricWriter.printMetric(metricPrefix + "|" + server.get("displayName") + "HeartBeat", "1", "AVERAGE", "AVERAGE", "INDIVIDUAL");
        } else {
            metricWriter.printMetric(metricPrefix + "|" + server.get("displayName") + "HeartBeat", "0", "AVERAGE", "AVERAGE", "INDIVIDUAL");
        }
    }

    public static class Builder {
        private OracledbMonitorTask task = new OracledbMonitorTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder server(Map server) {
            task.server = server;
            return this;
        }

        Builder jdbcAdapter(JDBCConnectionAdapter adapter) {
            task.jdbcAdapter = adapter;
            return this;
        }

        Builder previousTimestamp(long timestamp) {
            task.previousTimestamp = timestamp;
            return this;
        }

        Builder currentTimestamp(long timestamp) {
            task.currentTimestamp = timestamp;
            return this;
        }

        OracledbMonitorTask build() {
            return task;
        }
    }
}
