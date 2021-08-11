# Oracle Database Monitoring Extension


## Use Case
The Oracle Database is an object-relational database management system. 

The Oracle Database monitoring extension captures performance metrics from Oracle databases 
(version 10g and above) and displays them in AppDynamics. The extension comes with a standard set of queries 
that gather metrics based on the following categories.

-   Activity: Throughput over the last minute, such as transactions, SQL executes, I/O reads and writes, average active sessions, etc.
-   Resource Utilization: What database resources are currently in use (sessions, open cursors, shared pool, etc).
-   Efficiency: Ratios and response times as indicators of the instance's efficiency.

This extension also allows you to query your Oracle Database and view those results on the AppDynamics Metric browser
and hence allowing you to build dashboards and set Health Rules based on the output of the extension.

## Prerequisites

In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Java+Agent) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility). 
For more details on downloading these products, please visit [download.appdynamics.com](https://download.appdynamics.com/).

This is very essential in order to establish a connection with the Oracle DB to get the metrics.
The extension needs to be able to connect to Oracle DB in order to collect and send metrics. 
To do this, you will have to either establish a remote connection in between the extension and the product, 
or have an agent on the same machine running the product in order for the extension to collect and send the metrics.


The Oracle DB extension needs an Oracle user account on every Oracle instance that is to be monitored. You might use an existing account with appropriate rights; however, a dedicated account will be a better solution in terms of security and manageability.
    -   Example script for account creation (run this with a DBA user):

```
            CREATE USER appdynamics IDENTIFIED BY oracle;
            GRANT CREATE SESSION TO appdynamics;
            GRANT SELECT ANY DICTIONARY TO appdynamics;
```
## Installation
1. To build from source, clone this repository and run 'mvn clean install'. This will produce a OracleDBMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/oracle-monitoring-extension/releases).
2. Unzip the file OracleDBMonitor-[version].zip into `<MACHINE_AGENT_HOME>/monitors/`.
3. In the newly created directory "OracleDBMonitor", edit the config.yml configuring the parameters (See Configuration section below).
4. Download the oracle JDBC jar file and place it in the `<MACHINE_AGENT_HOME>/monitors/OracleDBMonitor` directory.
5. Edit the monitor.xml and provide the name of the jar file in the classpath. For eg. 
   
```   
    <classpath>oracle-monitoring-extension.jar;ojdbc6.jar</classpath>
```    

6. Restart the machineagent
7. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Oracle  .
8. If you're monitoring multiple Oracle DB instances, follow the above steps for every Oracle instance that you want to monitor.

**Note:** Please place the extension in the **"monitors"** directory of your **Machine Agent** installation 
directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.

## Configuration ##
Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Oracle DB parameters by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/OracleDBMonitor/`. 

   Here is a sample config.yml file
    ```
    # Make sure the metric prefix ends with a |
    #This will create this metric in all the tiers, under this path.
    metricPrefix: "Custom Metrics|OracleDB|"
    #This will create it in specific Tier. Replace <ComponentID> with TierID
    #metricPrefix: "Server|Component:<ComponentID>|Custom Metrics|OracleDB|"
    
    
    dbServers:
        - displayName: "OracleDB"
    #      connectionUrl: "jdbc:oracle:thin:username/password@HostForDatabase:PortForDatabase:databaseName"
          connectionUrl: "jdbc:oracle:thin:@HostForDatabase:PortForDatabase:databaseName"
    
          driver: "oracle.jdbc.OracleDriver"
    
          connectionProperties:
             - user: "system"
             - password: "oracle"
    
          #Needs to be used in conjunction with `encryptionKey`. Please read the extension documentation to generate encrypted password
    #      encryptedPassword: "9XdTa7+McBwP2g2xSpyNsg=="
    
          #Needs to be used in conjunction with `encryptedPassword`. Please read the extension documentation to generate encrypted password
    #      encryptionKey: "myKey"
    
          # Replaces characters in metric name with the specified characters.
          # "replace" takes any regular expression
          # "replaceWith" takes the string to replace the matched characters
    
          metricCharacterReplacer:
            - replace: "%"
              replaceWith: ""
            - replace: ","
              replaceWith: "-"
    
    
          queries:
            - displayName: "Query1 Sessions "
              queryStmt: "SELECT 'Sessions' , count(*) NumberOfSessions from v$session "
              columns:
                - name: "'Sessions'"
                  type: "metricPathName"
    
                - name: "NumberOfSessions"
                  type: "metricValue"
    
            - displayName: "Query2 Percent of Max Sessions"
              queryStmt: "SELECT 'Percent of max sessions' as sessionNumber, a.cnt / b.cpus * 100 AS Value FROM ( SELECT COUNT(*) cnt FROM v$session ) a, ( SELECT value AS cpus FROM v$parameter WHERE name='sessions') b"
              columns:
                - name: "sessionNumber"
                  type: "metricPathName"
    
                - name: "Value"
                  type: "metricValue"
    
            - displayName: "Query3 Percent of Max Open Cursors"
              queryStmt: "SELECT 'Percent of Max open cursors' as cursors, a.crs / b.max_crs * 100 as Value FROM ( SELECT MAX(a.value) AS crs from v$sesstat a, v$statname b where a.statistic# = b.statistic# AND b.name = 'opened cursors current' ) a, ( select value AS max_crs FROM v$parameter WHERE name='open_cursors' ) b"
              columns:
                - name: "cursors"
                  type: "metricPathName"
    
                - name: "Value"
                  type: "metricValue"
    
            - displayName: "Query4 Active User Sessions"
              queryStmt: "SELECT 'Active User Sessions' as ActiveUserSessions, COUNT(*) Count FROM v$session WHERE status='ACTIVE' AND username IS NOT NULL"
              columns:
                - name: "ActiveUserSessions"
                  type: "metricPathName"
    
                - name: "Count"
                  type: "metricValue"
    
            - displayName: "Query5 Avg Active Sessions Per Logical CPU"
              queryStmt: "SELECT 'Average Active Sessions per logical CPU' as AvgActive, a.value / b.cpus AS Value FROM (SELECT value FROM v$sysmetric WHERE group_id = 2 AND metric_name = 'Average Active Sessions') a, (SELECT value AS cpus FROM v$parameter WHERE name='cpu_count') b"
              columns:
                - name: "AvgActive"
                  type: "metricPathName"
    
                - name: "Value"
                  type: "metricValue"
    
            - displayName: "Query6 System Metrics"
              queryStmt: "SELECT metric_name, Value FROM v$sysmetric WHERE group_id = 2 AND metric_name IN ('Average Active Sessions', 'Current OS Load', 'Database CPU Time Ratio', 'Database Wait Time Ratio', 'DB Block Changes Per Sec', 'DB Block Changes Per Txn', 'DB Block Gets Per Sec', 'DB Block Gets Per Txn', 'Executions Per Sec', 'Executions Per Txn', 'I/O Megabytes per Second', 'Logical Reads Per Sec', 'Physical Reads Per Sec', 'Memory Sorts Ratio', 'Physical Read Total Bytes Per Sec', 'Physical Write Total Bytes Per Sec', 'Shared Pool Free %', 'Execute Without Parse Ratio', 'Soft Parse Ratio', 'Temp Space Used', 'Total PGA Allocated', 'Response Time Per Txn', 'SQL Service Response Time') ORDER BY metric_name"
              columns:
                - name: "metric_name"
                  type: "metricPathName"
    
                - name: "Value"
                  type: "metricValue"
    
            - displayName: "Query7 Wait Class BreakDown Metrics"
              queryStmt: "SELECT 'Wait Class Breakdown|'||wait_class as waitingMetric, ROUND(aas, 2) as Value FROM(SELECT n.wait_class, m.time_waited/m.INTSIZE_CSEC AAS FROM v$waitclassmetric m, v$system_wait_class n WHERE m.wait_class_id=n.wait_class_id AND n.wait_class != 'Idle' UNION ALL SELECT 'CPU', value/100 AAS FROM v$sysmetric WHERE metric_name = 'CPU Usage Per Sec' AND group_id = 2)"
              columns:
                - name: "waitingMetric"
                  type: "metricPathName"
    
                - name: "Value"
                  type: "metricValue"
    
            - displayName: "Query8 Table Space Percent Free"
              queryStmt: "select df.tablespace_name as tableName, round(100 * ( (df.totalspace - tu.totalusedspace)/ df.totalspace)) Value from (select tablespace_name, round(sum(bytes) / 1048576) totalSpace from dba_data_files group by tablespace_name) df, (select round(sum(bytes)/(1024*1024)) totalusedspace, tablespace_name from dba_segments group by tablespace_name) tu where df.tablespace_name = tu.tablespace_name"
              columns:
                - name: "tableName"
                  type: "metricPathName"
    
                - name: "Value"
                  type: "metricValue"
    
    
    numberOfThreads: 5
    
    #Run it as a scheduled task instead of running every minute.
    #If you want to run this every minute, comment this out
    # taskSchedule:
    #   numberOfThreads: 1
    #   taskDelaySeconds: 10
    
    ```

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/OracleDBMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/OracleDBMonitor/config.yml" />
          ....
     </task-arguments>
    ```
    **Note:** You will need to provide your own **JDBC** driver for the database you want to connect to. Put the driver JAR file in the same directory and add it to the classpath element in the
monitor.xml file.!

### Example ###
```
<java-task>
    <!-- Use regular classpath foo.jar;bar.jar -->
    <!-- append JDBC driver jar -->
    <classpath>oracledb-monitoring-extension.jar;Jar-File-For_Your-DB.jar</classpath>
    <impl-class>com.appdynamics.extensions.oracledb.OracleDBMonitor</impl-class>
</java-task>
```

4. Restart the Machine Agent.


#### How to Connect to your Database with the extension ####
Lets take a look at some sample connection information: 
```
dbServers:
dbServers:
    - displayName: "OracleDB1"
      connectionUrl: "jdbc:oracle:thin:system/oracle@192.168.57.106:1521:orcl12c"
      driver: "oracle.jdbc.OracleDriver"
      
#      connectionProperties:
#         - user: "system"
#         - password: "oracle"

```
In order to connect to any database, you will have to provide a connectionUrl. 
In the example above we see that the extension is connected to the database **(orcl12c)**(listed in the config) using the connectionUrl. 
In this case we are also providing the username, password and the databaseName in the same connectionUrl 
and therefore the "connectionProperties" and the fields under it, "user" and "password", are commented out. 
You have to make sure that if you are not sending any connectionProperties to create a 
connection, then you should comment the whole thing out just like in the example. 

Lets take a look at another way you can connect to the database.
In this case we do need to provide properties such as a username and a password and 
therefore we uncomment those lines and update them with valid information.

```
dbServers:
    - displayName: "OracleDB1"
      connectionUrl: "jdbc:oracle:thin:@192.168.57.106:1521:orcl12c"
      driver: "oracle.jdbc.OracleDriver"

      connectionProperties:
         - user: "system"
         - password: "oracle"
```
In this case we do add the Database Name as the last part of the connectionUrl **(orcl12c)** but all other properties like the **username** and **password** are provided as **connectionProperties**. 
You will have to confirm how your database takes in the login information and based on that provide the information in your config.yaml in order to successfully establish a connection.


#### Explanation of the type of queries that are supported with this extension ####
Only queries that start with **SELECT** are allowed! Your query should only return one row at a time. 

It is suggested that you only  return one row at a time because if it returns a full table with enormous amount of data, it may overwhelm the system and it may take a very long time to fetch that data.  

The extension does support getting values from multiple columns at once but it can only pull the metrics from the latest value from the row returned.

The name of the metric displayed on the **Metric Browser** will be the "name" value that you specify in the config.yml for that metric. 
Looking at the following sample query : 

```
 queries:
   - displayName: "Active Events"
     queryStmt: "Select NODE_NAME, EVENT_CODE, EVENT_ID, EVENT_POSTED_COUNT from Active_events"
     columns:
       - name: "NODE_NAME"
         type: "metricPathName"

       - name: "EVENT_ID"
         type: "metricPathName"

       - name: "EVENT_CODE"
         type: "metricValue"

       - name: "EVENT_POSTED_COUNT"
         type: "metricValue"

```

1. **queries** : You can add multiple queries under this field. 
    1. **displayName** : The name you would like to give to the metrics produced by this query. 
    2. **queryStmt** : This will be your SQL Query that will be used to query the database.
    3. **columns**: Under this field you will have to list all the columns that you are trying to get values from.
        1. **name** : The name of the column you would like to see on the metric browser.
        2. **type** : This value will define if the value returned from the column will be used for the name of the metric or if it is going to be the value of the metric.
            1. **metricPathName** : If you select this, this value will be added to the metric path for the metric.
            2. **metricValue** : If you select this, then the value returned will become your metric value that will correspond to the name you specified above.
            
For the query listed above, there will be two metrics returned as we have two columns of type "metricValue".
The metric path for them will be : 
1. Custom Metrics|SQL|Instance1|Active Events|NODE_NAME|EVENT_ID|EVENT_CODE
2. Custom Metrics|SQL|Instance1|Active Events|NODE_NAME|EVENT_ID|EVENT_POSTED_COUNT

Lets look at another query.
```
           - displayName: "Node Status"
             queryStmt: "Select NODE_NAME, NODE_STATE from NODE_STATES"
             columns:
               - name: "NODE_NAME"
                 type: "metricPathName"
   
               - name: "NODE_STATE"
                 type: "metricValue"
                 properties:
                   convert:
                     "INITIALIZING" : 0
                     "UP" : 1
                     "DOWN" : 2
                     "READY" : 3
                     "UNSAFE" : 4
                     "SHUTDOWN" : 5
                     "RECOVERING" : 6

```
Lets say if your query returns a text value, but you would still like to see it in the metric browser. 
In order to make that happen, you could use the **"convert"** property and assign each value a number. 
The extension will automatically convert the text value to the corresponding number.

**NOTE:** In order to use this feature, please make sure that the value that is being returned is EXACTLY the same as you have listed in the config.yaml, otherwise the extension will throw an error.

### Oracle Licensing
The metrics in the supplied code are retrieved from

```
-   v$session
-   v$sesstat
-   v$sysmetric
-   v$system_wait_class
-   v$waitclassmetric
```

all of which are, to the author's knowledge, not subject to additional
licensing of the Oracle Diagnostics Pack. See Oracle's "Options and
Packs" documentation:
[https://docs.oracle.com/cd/B28359_01/license.111/b28287/options.htm#DBLIC138](https://docs.oracle.com/cd/B28359_01/license.111/b28287/options.htm#DBLIC138)

If you plan on extending this code using data dictionary views of the
Diagnostics Pack (e.g., DBA\_HIST\_% views), you might want to make use
of the argument "ash\_licensed" in monitor.xml to easily en-/disable
usage of such code.

## Metrics
Here is a summary of the collected metrics. Complete documentation of Oracle's website.

AppDynamics displays metric values as integers. Some metrics are therefore scaled up by a factor of 100 for a better display of low values (e.g. between 0 and 2).


<table>
<tr><td><strong>Metric Class</strong></td><td><strong>Description</strong></td>

<tr>
<td>Activity</td>
<td>

<table>
    <tr><td><strong>Metric</strong></td><td><strong>Description</strong></td>
    <tr><td>Active Sessions Current</td><td>Number of active sessions at the point in time when the snapshot was taken.</td></tr>
    <tr><td>Average Active Sessions</td><td>Average number of active sessions within the last 60 s. This is maybe the single most important DB load metric and a good starting point for a drill-down.</td></tr>
    <tr><td>Average Active Sessions per logical CPU (\*100)</td><td>This shows the average load the database imposes on each logical CPU (i.e. cores or hyperthreads). Values above 100 (more than 1 waiting DB session per CPU) indicate a higher demand for resources than the host can satisfy. This often marks the beginning of quickly rising response times.</td></tr>
    <tr><td>Current OS Load</td><td>Host CPU load, when available.</td></tr>
    <tr><td> DB Block Changes Per Sec</td><td>Database blocks changed in the buffer cache.</td></tr>
    <tr><td>DB Block Changes Per Txn</td><td>Database blocks changed in the buffer cache per SQL transaction.</td></tr>
    <tr><td>DB Block Gets Per Sec</td><td>Database blocks read from the buffer cache.</td></tr>
    <tr><td>DB Block Gets Per Txn</td><td>Database blocks read from the buffer cache per SQL transaction.</td></tr>
    <tr><td>Executions Per Sec</td><td>SQL executions/s</td></tr>
    <tr><td>Executions Per Txn</td><td>SQL executions per SQL transaction.</td></tr>
    <tr><td>I/O Megabytes per Second</td> <td></td></tr>
    <tr><td>Logical Reads Per Sec</td><td>Logical reads are comprised of database block reads from the buffer cache + physical reads from disk.</td></tr>
    <tr><td>Logons Per Sec</td> <td></td></tr>
    <tr><td>Physical Reads Per Sec</td><td>Database blocks read from disk.</td></tr>
    <tr><td>Physical Read Total Bytes Per Sec</td> <td></td></tr>
    <tr><td>Physical Write Total Bytes Per Sec</td> <td></td></tr>
    <tr><td>Txns Per Sec</td><td>Transactions per second.</td> <td></td></tr>
</table>
  
</td>
</tr>

<tr>
<td>Wait Class Breakdown <a name = "waitclassbreakdown"></a></td>
<td>Shows average active sessions per each wait class. Typically, the top wait classes are 
"CPU" and "User I/O". A shift to other wait classes is a good pointer for further investigation
 (e.g., of network latency issues). Wait classes are documented in the Oracle Database Reference.
  See here: https://docs.oracle.com/cloud/latest/db112/REFRN/toc.htm</td>
</tr>


<tr>
  <td>Efficiency<a name = "efficiency"></a></td>
  <td>
  
<table>
    <tr><td><strong>Metric</strong></td><td><strong>Description</strong></td></tr>
    <tr><td>Database CPU Time Ratio</td><td>Percentage of CPU time against all database time.</td></tr>
    <tr><td>Database Wait Time Ratio</td><td>Complementary to "Database CPU Time Ratio" (percentage of non-CPU waits).</td></tr>
    <tr><td>Memory Sorts Ratio</td><td>Percentage of sort operations that were done in RAM (as opposed to disk).</td></tr>
    <tr><td>Execute Without Parse Ratio</td><td>Percentage of (soft and hard) parsed SQL against all executed SQL.</td></tr>
    <tr><td>Soft Parse Ratio</td><td>Ratio of soft parses to hard parses.</td></tr>
    <tr><td>Response Time Per Txn (ms)</td> <td></td></tr>
    <tr><td>SQL Service Response Time (ms) <td></td> </tr>
</table>
    
  </td>
</tr>

<tr><td>Resource Utilization<a name="resourceutilization"></a></td>
<td>

<table>
    <tr><td><strong>Metric</strong></td><td><strong>Description</strong></td></tr>
    <tr><td> \# of logical CPUs</td><td>Observation for informational purpose. This count is used, among others, for the metric "Average Active Sessions per logical CPU".</td>
    <tr><td>Total Sessions</td><td>Count of all database sessions at the time the snapshot was taken.</td>
    <tr><td>% of max sessions</td><td>Open sessions vs. DB parameter "sessions".</td>
    <tr><td>% of max open cursors</td><td>Maximum count of open cursors in a session vs. DB parameter "open\_cursors".</td>
    <tr><td>Shared Pool Free %</td> <td></td>
    <tr><td>Temp Space Used (MB)</td><td>Amount of used temporary tablespace.</td>
    <tr><td>Total PGA Allocated (MB)</td><td>Amount of RAM used for sorts, hashes and the like.
  </table>
  </td>
</tr>

</table>




## Credentials Encryption
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) page to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following [document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130) for how to use the Extensions WorkBench

## Troubleshooting
1. Please follow the steps listed in the [extensions troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the troubleshooting-document to contact the support team.
2. If you are seeing ``` java.lang.NoClassDefFoundError: org/ietf/jgss/GSSException```  please add the jgss.jar in the <MA_HOME>/monitorsLibs and restart Machine agent.

## Support Tickets
If after going through the Troubleshooting Document you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.  

1. Stop the running machine agent .
2. Delete all existing logs under <MachineAgent>/logs .
3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug. 
   ```
   <logger name="com.singularity">
   <logger name="com.appdynamics">
     ```
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
5. Attach the zipped <MachineAgent>/conf/* directory here.
 6. Attach the zipped <MachineAgent>/monitors/<ExtensionMonitor> directory here .

For any support related questions, you can also contact help@appdynamics.com.

## Contributing
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/vertica-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.5.0       |
|Controller Compatibility  |4.5 or Later|
|Product Tested On         |OracleDB |
|Last Update               |10/08/2021 |
|List of Changes           |[Change log](https://github.com/Appdynamics/oracle-monitoring-extension/blob/master/CHANGELOG.md) |
