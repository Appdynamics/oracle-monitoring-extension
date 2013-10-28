/**
 * Copyright 2013 AppDynamics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




package com.appdynamics.monitors.oraclemonitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.appdynamics.monitors.oraclemonitor.common.JavaServersMonitor;

import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

// activate Debug Level locally
//import org.apache.log4j.Level;


public class OraDbMonitor extends JavaServersMonitor
{
	Connection conn = null;
    private volatile String tierName;
    private volatile String sid;
    private volatile Boolean ashLicensed = false;
	
	public OraDbMonitor() throws ClassNotFoundException {
		oldValueMap = Collections.synchronizedMap(new HashMap<String, String>());
	    Class.forName("oracle.jdbc.driver.OracleDriver");
	}
	
	protected void parseArgs(Map<String, String> args)
	{
		super.parseArgs(args);
	    tierName = getArg(args, "tier", null); // if the tier is not specified then create the metrics for all tiers
	    sid = getArg(args, "sid", null);
	    ashLicensed = (getArg(args, "ash_licensed", "false").equals("true"));
	    logger.debug("Parsed args: " + args);
	}

	private Connection connect() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		String connStr = "jdbc:oracle:thin:@" + host + ":";
		if ((port == null) || (port.equals("")))
			connStr += "1521";
		else
			connStr += port;

	    connStr += ":";
        
        if ((sid == null) || (sid.equals("")))
	        connStr += "orcl";
	    else
	        connStr += sid;

        logger.debug("Connecting to: " + connStr);
	    Connection conn = DriverManager.getConnection(connStr, userName, passwd);
        logger.debug("Successfully connected to Oracle DB");

		return conn;
	}

	// collects all monitoring data for this time period from database
	private void populate(Map<String, String> valueMap, String[] queries) throws Exception
	{
		Connection conn = null;
		Statement stmt = null;
		
		boolean debug = logger.isDebugEnabled();
		
		try
		{
			conn = connect();
			stmt = conn.createStatement();
			
			for (String query : queries) 
			{
				ResultSet rs = null;
				
				try 
				{
					if (debug)
					{
						logger.debug("Executing query ["+query+"]");
					}
					
					rs = stmt.executeQuery(query);

					while (rs.next())
					{
						String key = rs.getString(1);
						String value = rs.getString(2);

						if (debug)
						{
							logger.debug("[key,value] = ["+key+","+value+"]");
						}
						
						valueMap.put(key.toUpperCase(), value);
					}
				} 
				catch (Exception ex) 
				{
					logger.error("Error while executing query ["+query+"]", ex);
					throw ex;
				}
				finally
				{
					close(rs, null, null);
				}
			}
			
			// get most accurate time
			currentTime = System.currentTimeMillis();
		}
		finally
		{
			close(null, stmt, conn);
		}
	}

	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
			throws TaskExecutionException
	{
		startExecute(taskArguments, taskContext);
                // activate debug level locally
                //logger.getRootLogger().setLevel(Level.DEBUG);

		try
		{
			populate(valueMap, new String[]{
            "select \'Sessions\', count(*) from v$session"
            , "select \'Active User Sessions\', COUNT(*) FROM v$session WHERE status=\'ACTIVE\' AND username IS NOT NULL"
		    , "select \'% of max open cursors\', "
              + "a.crs / b.max_crs * 100 as pct_open_cur" + 
              "  FROM ( SELECT MAX(a.value) AS crs from v$sesstat a, v$statname b where a.statistic# = b.statistic# " + 
              "            AND b.name = \'opened cursors current\' ) a" + 
              "     , ( select value AS max_crs FROM v$parameter WHERE name=\'open_cursors\' ) b"
		    , "select \'% of max sessions\', "
              + "a.cnt / b.cpus * 100 AS pct_max_sessions" + 
              "  FROM ( SELECT COUNT(*) cnt FROM v$session ) a" + 
              "     , ( SELECT value AS cpus FROM v$parameter WHERE name=\'sessions\' ) b"
		    , "SELECT metric_name, value\n" + 
		    "  FROM v$sysmetric\n" + 
		    " WHERE group_id = 2 -- 60 sec interval\n" + 
		    "   AND metric_name IN (\n" + 
		    "         'Average Active Sessions'\n" + 
		    "       , 'Current OS Load'\n" + 
		    "       , 'Database CPU Time Ratio'\n" + 
		    "       , 'Database Wait Time Ratio'\n" + 
		    "       , 'DB Block Changes Per Sec'\n" + 
		    "       , 'DB Block Changes Per Txn'\n" + 
		    "       , 'DB Block Gets Per Sec'\n" + 
		    "       , 'DB Block Gets Per Txn'\n" + 
		    "       , 'Executions Per Sec'\n" + 
		    "       , 'Executions Per Txn'\n" + 
		    "       , 'I/O Megabytes per Second'\n" + 
		    "       , 'Logical Reads Per Sec'\n" + 
		    "       , 'Physical Reads Per Sec'\n" + 
		    "       , 'Memory Sorts Ratio'\n" + 
		    "       , 'Physical Read Total Bytes Per Sec'\n" + 
		    "       , 'Physical Write Total Bytes Per Sec'\n" + 
		    "       , 'Shared Pool Free %'\n" + 
		    "       , 'Execute Without Parse Ratio'\n" + 
		    "       , 'Soft Parse Ratio'\n" + 
		    "       , 'Temp Space Used'\n" + 
		    "       , 'Total PGA Allocated'\n" + 
		    "       , 'Response Time Per Txn'\n" + 
		    "       , 'SQL Service Response Time'\n" + 
		    "  )\n" + 
		    " ORDER BY metric_name"
		    , "SELECT 'Average Active Sessions per logical CPU', a.value / b.cpus AS aas_per_cpu\n" + 
		    "  FROM ( SELECT value FROM v$sysmetric WHERE group_id = 2\n" + 
		    "            AND metric_name = 'Average Active Sessions' ) a\n" + 
            "     , ( SELECT value AS cpus FROM v$parameter WHERE name=\'cpu_count\' ) b"
            , "SELECT 'Wait Class Breakdown|'||wait_class, ROUND( aas, 2 ) FROM(\n" + 
            "SELECT n.wait_class, m.time_waited/m.INTSIZE_CSEC AAS\n" + 
            "  FROM v$waitclassmetric m\n" + 
            "     , v$system_wait_class n\n" + 
            " WHERE m.wait_class_id=n.wait_class_id\n" + 
            "   AND n.wait_class != 'Idle'\n" + 
            "UNION ALL\n" + 
            "SELECT 'CPU', value/100 AAS\n" + 
            "  FROM v$sysmetric\n" + 
            " WHERE metric_name = 'CPU Usage Per Sec'\n" + 
            "   AND group_id = 2\n" + 
            ")"
		    // ASH + AWR ///////////////////////////////////////////////
		    // Currently, all metrics can be retrieved w/o using ASH/AWR
		    //if (ashLicensed){
		    //    // select stuff from ASH
		    //}
            });
		}
		catch (Exception ex)
		{
			throw new TaskExecutionException(ex);
		}

		// just for debug output
		// you may want to revert back to "debug"
		logger.info("Starting METRIC UPLOAD for Oracle Monitor.......");

		// RESOURCE UTILIZATION ////////////////////////////////////
        printMetric("Resource Utilization|Total Sessions", getString("Sessions"),
            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Resource Utilization|% of max sessions", getString("% of max sessions"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Resource Utilization|% of max open cursors", getString("% of max open cursors"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Resource Utilization|Shared Pool Free %", getString("Shared Pool Free %"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Resource Utilization|Temp Space Used", getString("Temp Space Used"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Resource Utilization|Total PGA Allocated", getString("Total PGA Allocated"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    // ACTIVITY ////////////////////////////////////////////////
	    printMetric("Activity|Active Sessions Current", getString("Active User Sessions"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Average Active Sessions", getString("Average Active Sessions"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Average Active Sessions per logical CPU", getString("Average Active Sessions per logical CPU"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Current OS Load", getString("Current OS Load"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|DB Block Changes Per Sec", getString("DB Block Changes Per Sec"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|DB Block Changes Per Txn", getString("DB Block Changes Per Txn"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|DB Block Gets Per Sec", getString("DB Block Gets Per Sec"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|DB Block Gets Per Txn", getString("DB Block Gets Per Txn"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Executions Per Sec", getString("Executions Per Sec"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Executions Per Txn", getString("Executions Per Txn"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|I/O Megabytes per Second", getString("I/O Megabytes per Second"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Logical Reads Per Sec", getString("Logical Reads Per Sec"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Physical Reads Per Sec", getString("Physical Reads Per Sec"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Physical Read Total Bytes Per Sec", getString("Physical Read Total Bytes Per Sec"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Physical Write Total Bytes Per Sec", getString("Physical Write Total Bytes Per Sec"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Administrative", getString("Wait Class Breakdown|Administrative"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Application", getString("Wait Class Breakdown|Application"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Commit", getString("Wait Class Breakdown|Commit"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Concurrency", getString("Wait Class Breakdown|Concurrency"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Configuration", getString("Wait Class Breakdown|Configuration"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|CPU", getString("Wait Class Breakdown|CPU"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Network", getString("Wait Class Breakdown|Network"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Other", getString("Wait Class Breakdown|Other"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|Scheduler", getString("Wait Class Breakdown|Scheduler"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|System I/O", getString("Wait Class Breakdown|System I/O"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Activity|Wait Class Breakdown|User I/O", getString("Wait Class Breakdown|User I/O"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    // EFFICIENCY //////////////////////////////////////////////
	    printMetric("Efficiency|Database CPU Time Ratio", getString("Database CPU Time Ratio"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Efficiency|Database Wait Time Ratio", getString("Database Wait Time Ratio"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Efficiency|Memory Sorts Ratio", getString("Memory Sorts Ratio"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Efficiency|Execute Without Parse Ratio", getString("Execute Without Parse Ratio"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    printMetric("Efficiency|Soft Parse Ratio", getString("Soft Parse Ratio"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

        // Time measured in Centiseconds
	    printMetric("Efficiency|Response Time Per Txn", getString("Response Time Per Txn"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

	    // Time measured in Centiseconds
	    printMetric("Efficiency|SQL Service Response Time", getString("SQL Service Response Time"),
	        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
	        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

        // ASH + AWR ///////////////////////////////////////////////
        // Currently, all metrics can be retrieved w/o using ASH/AWR
        if (ashLicensed){
            logger.debug("licensed: ASH + AWR");
        }
        
        logger.info("METRIC UPLOAD for Oracle Monitor done.");

		return this.finishExecute();
	}

	protected String getMetricPrefix()
	{
		if (tierName != null)
		{
			return "Server|Component:"+tierName+"|ORACLE Server|";
		}
		else
		{	
			return "Custom Metrics|ORACLE Server|";
		}
	}
	
}
