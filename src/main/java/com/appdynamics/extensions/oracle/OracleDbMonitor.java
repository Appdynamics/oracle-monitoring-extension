/*
 * Copyright 2013. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.oracle;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.oracle.common.OracleQueries;
import com.appdynamics.extensions.oracle.config.Configuration;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OracleDbMonitor extends AManagedMonitor {
    private static final Logger logger = Logger.getLogger(OracleDbMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    public static final String DUMP_FOR_PRINTING = "dumpForPrinting";
    private Multimap<String,String> valueMap;

    public OracleDbMonitor() throws ClassNotFoundException {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        System.out.println(msg);
        Class.forName("oracle.jdbc.driver.OracleDriver");
    }

    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
            throws TaskExecutionException {
        if(taskArguments != null) {
            logger.info("Starting " + getImplementationVersion() + " Monitoring Task");
            try {
                String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
                Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);

                fetchMetrics(config, OracleQueries.queries);

                printDBMetrics(config);

                logger.info("Oracle DB Monitoring Task completed successfully");
                return new TaskOutput("Oracle DB Monitoring Task completed successfully");
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("Oracle DB Monitoring Task completed with failures.");
    }

    private void fetchMetrics(Configuration config, String[] queries) throws Exception {
        valueMap = ArrayListMultimap.create();
        Connection conn = null;
        Statement stmt = null;
        boolean debug = logger.isDebugEnabled();
        try {
            conn = connect(config);
            stmt = conn.createStatement();
            for (String query : queries) {
                ResultSet rs = null;
                try {
                    if (debug) {
                        logger.debug("Executing query [" + query + "]");
                    }
                    rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        String key = rs.getString(1);
                        String value = rs.getString(2);
                        if (debug) {
                            logger.debug("[key,value] = [" + key + "," + value + "]");
                        }
                        valueMap.put(key.toUpperCase(), value);
                        //dump metrics for printing
                        if(query.equalsIgnoreCase(OracleQueries.tableSpacePctFree)){
                            valueMap.put(key.toUpperCase(), DUMP_FOR_PRINTING);
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Error while executing query [" + query + "]", ex);
                    throw ex;
                } finally {
                    close(rs, null, null);
                }
            }
        } finally {
            close(null, stmt, conn);
        }
    }

    private Connection connect(Configuration config) throws SQLException {
        String host = config.getHost();
        String port = String.valueOf(config.getPort());
        String userName = config.getUsername();
        String password = config.getPassword();
        String sid = config.getSid();

        if(Strings.isNullOrEmpty(port)) {
            port = "1521";
        }
        if(Strings.isNullOrEmpty(sid)) {
            sid = "orcl";
        }
        String connStr = String.format("jdbc:oracle:thin:@%s:%s:%s", host, port, sid);
        logger.debug("Connecting to: " + connStr);

        Connection conn = DriverManager.getConnection(connStr, userName, password);
        logger.debug("Successfully connected to Oracle DB");

        return conn;
    }



    private void printDBMetrics(Configuration config) {
        String metricPath = config.getMetricPathPrefix() + config.getSid() + "|";
        // RESOURCE UTILIZATION ////////////////////////////////////
        String resourceUtilizationMetricPath = metricPath + "Resource Utilization|";
        printMetric(resourceUtilizationMetricPath + "Total Sessions", getString("Sessions"));
        printMetric(resourceUtilizationMetricPath + "% of max sessions", getString("% of max sessions"));
        printMetric(resourceUtilizationMetricPath + "% of max open cursors", getString("% of max open cursors"));

        printMetric(resourceUtilizationMetricPath + "Shared Pool Free %", getString("Shared Pool Free %"));
        printMetric(resourceUtilizationMetricPath + "Temp Space Used", getString("Temp Space Used"));
        printMetric(resourceUtilizationMetricPath + "Total PGA Allocated", getString("Total PGA Allocated"));

        // ACTIVITY ////////////////////////////////////////////////
        String activityMetricPath = metricPath + "Activity|";
        printMetric(activityMetricPath + "Active Sessions Current", getString("Active User Sessions"));
        printMetric(activityMetricPath + "Average Active Sessions per logical CPU", getString("Average Active Sessions per logical CPU"));

        printMetric(activityMetricPath + "Average Active Sessions", getString("Average Active Sessions"));
        printMetric(activityMetricPath + "Current OS Load", getString("Current OS Load"));
        printMetric(activityMetricPath + "DB Block Changes Per Sec", getString("DB Block Changes Per Sec"));
        printMetric(activityMetricPath + "DB Block Changes Per Txn", getString("DB Block Changes Per Txn"));
        printMetric(activityMetricPath + "DB Block Gets Per Sec", getString("DB Block Gets Per Sec"));
        printMetric(activityMetricPath + "DB Block Gets Per Txn", getString("DB Block Gets Per Txn"));
        printMetric(activityMetricPath + "Executions Per Sec", getString("Executions Per Sec"));
        printMetric(activityMetricPath + "Executions Per Txn", getString("Executions Per Txn"));
        printMetric(activityMetricPath + "I/O Megabytes per Second", getString("I/O Megabytes per Second"));
        printMetric(activityMetricPath + "Logical Reads Per Sec", getString("Logical Reads Per Sec"));
        printMetric(activityMetricPath + "Physical Reads Per Sec", getString("Physical Reads Per Sec"));
        printMetric(activityMetricPath + "Physical Read Total Bytes Per Sec", getString("Physical Read Total Bytes Per Sec"));
        printMetric(activityMetricPath + "Physical Write Total Bytes Per Sec", getString("Physical Write Total Bytes Per Sec"));

        printMetric(activityMetricPath + "Wait Class Breakdown|Administrative", getString("Wait Class Breakdown|Administrative"));
        printMetric(activityMetricPath + "Wait Class Breakdown|Application", getString("Wait Class Breakdown|Application"));
        printMetric(activityMetricPath + "Wait Class Breakdown|Commit", getString("Wait Class Breakdown|Commit"));
        printMetric(activityMetricPath + "Wait Class Breakdown|Concurrency", getString("Wait Class Breakdown|Concurrency"));
        printMetric(activityMetricPath + "Wait Class Breakdown|Configuration", getString("Wait Class Breakdown|Configuration"));
        printMetric(activityMetricPath + "Wait Class Breakdown|CPU", getString("Wait Class Breakdown|CPU"));
        printMetric(activityMetricPath + "Wait Class Breakdown|Network", getString("Wait Class Breakdown|Network"));
        printMetric(activityMetricPath + "Wait Class Breakdown|Other", getString("Wait Class Breakdown|Other"));
        printMetric(activityMetricPath + "Wait Class Breakdown|Scheduler", getString("Wait Class Breakdown|Scheduler"));
        printMetric(activityMetricPath + "Wait Class Breakdown|System I/O", getString("Wait Class Breakdown|System I/O"));
        printMetric(activityMetricPath + "Wait Class Breakdown|User I/O", getString("Wait Class Breakdown|User I/O"));

        // EFFICIENCY //////////////////////////////////////////////
        String efficiencyMetricPath = metricPath + "Efficiency|";
        printMetric(efficiencyMetricPath + "Database CPU Time Ratio", getString("Database CPU Time Ratio"));
        printMetric(efficiencyMetricPath + "Database Wait Time Ratio", getString("Database Wait Time Ratio"));
        printMetric(efficiencyMetricPath + "Memory Sorts Ratio", getString("Memory Sorts Ratio"));
        printMetric(efficiencyMetricPath + "Execute Without Parse Ratio", getString("Execute Without Parse Ratio"));
        printMetric(efficiencyMetricPath + "Soft Parse Ratio", getString("Soft Parse Ratio"));
        // Time measured in Centiseconds
        printMetric(efficiencyMetricPath + "Response Time Per Txn", getString("Response Time Per Txn"));
        // Time measured in Centiseconds
        printMetric(efficiencyMetricPath + "SQL Service Response Time", getString("SQL Service Response Time"));
        // table space metrics
        String tableSpaceMetricPath = metricPath + "TableSpaceMetrics|";
        for(Map.Entry<String,Collection<String>> entry : valueMap.asMap().entrySet()){
            if(entry.getValue().contains(DUMP_FOR_PRINTING)){
                printMetric(tableSpaceMetricPath + entry.getKey() + "|Free %",getString(entry.getKey()));
            }
        }
    }

    protected void printMetric(String metricName, String value) {
        if(!Strings.isNullOrEmpty(value)) {
            try {
                MetricWriter metricWriter = getMetricWriter(metricName, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                metricWriter.printMetric(value);
                //System.out.println(metricName + "  " + value);
                if (logger.isDebugEnabled()) {
                    logger.debug("METRIC:  NAME:" + metricName + " VALUE:" + value);
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    protected void close(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    protected String getString(float num) {
        int result = Math.round(num);
        return Integer.toString(result);
    }

    // lookup value for key, convert to float, round up or down and then return as string form of int
    protected String getString(String key) {
        return getString(key, true);
    }

    // specify whether to convert this key to uppercase before looking up the value
    protected String getString(String key, boolean convertUpper) {
        if (convertUpper)
            key = key.toUpperCase();
        List<String> values = (List<String>) valueMap.get(key);
        //multi-map never returns null, an empty collection.
        if(values.size() < 1){
            return "";
        }
        String strResult = values.get(0);
        if (strResult == null) {
            return "";
        }

        // round the result to a integer since we don't handle fractions
        float result = Float.valueOf(strResult);
        String resultStr = getString(result);
        return resultStr;
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        //for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    public static String getImplementationVersion() {
        return OracleDbMonitor.class.getPackage().getImplementationTitle();
    }

    public static void main(String[] args) {
        System.out.println("Using Monitor Version [" + getImplementationVersion() + "]");
    }
}
