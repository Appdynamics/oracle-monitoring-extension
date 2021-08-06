/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.oracledb;

import com.appdynamics.extensions.ABaseMonitor;
//import com.appdynamics.extensions.TaskInputArgs;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.util.CryptoUtils;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import static com.appdynamics.extensions.oracledb.Constant.METRIC_PREFIX;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//import static com.appdynamics.extensions.TaskInputArgs.PASSWORD_ENCRYPTED;

/**
 * Created by bhuvnesh.kumar on 1/23/18.
 */
public class OracledbMonitor extends ABaseMonitor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(OracledbMonitor.class);
    private long previousTimestamp = 0;
    private long currentTimestamp = System.currentTimeMillis();
    private static final String CONFIG_ARG = "config-file";

    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "OracleDB Monitor";
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {

        List<Map<String, ?>> servers = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("dbServers");


        previousTimestamp = currentTimestamp;
        currentTimestamp = System.currentTimeMillis();
        if (previousTimestamp != 0) {
            for (Map<String, ?> server : servers) {
                try {
                    OracledbMonitorTask task = createTask(server, serviceProvider);
                    serviceProvider.submit((String) server.get("displayName"), task);
                } catch (Exception e) {
                    logger.error("Error while creating task for {}", Util.convertToString(server.get("displayName"), ""),e);
                }
            }
        }
    }

    // @Override
    // protected int getTaskCount() {
    //     List<Map<String, String>> servers = (List<Map<String, String>>) configuration.getConfigYml().get("dbServers");
    //     return servers.size();
    // }

    @Override
    protected List<Map<String, ?>> getServers() {
        return (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("dbServers");
    }



    private OracledbMonitorTask createTask(Map<String, ?> server, TasksExecutionServiceProvider serviceProvider) {
        String connUrl = createConnectionUrl(server);

        //Fix for ACE-1001
        if(Strings.isNullOrEmpty(serverName(server))){
            throw new IllegalArgumentException("The 'displayName' field under the 'dbServers' section in config.yml is not initialised");
        }
        if(Strings.isNullOrEmpty(createConnectionUrl(server))){
            throw new IllegalArgumentException("The 'connectionUrl' field under the 'dbServers' section in config.yml is not initialised");
        }
        if(Strings.isNullOrEmpty(driverName(server))){
            throw new IllegalArgumentException("The 'driver' field under the 'dbServers' section in config.yml is not initialised");
        }

//        AssertUtils.assertNotNull(serverName(server), "The 'displayName' field under the 'dbServers' section in config.yml is not initialised");
//        AssertUtils.assertNotNull(createConnectionUrl(server), "The 'connectionUrl' field under the 'dbServers' section in config.yml is not initialised");
//        AssertUtils.assertNotNull(driverName(server), "The 'driver' field under the 'dbServers' section in config.yml is not initialised");
        Map<String, String> connectionProperties = getConnectionProperties(server);
        JDBCConnectionAdapter jdbcAdapter = JDBCConnectionAdapter.create(connUrl, connectionProperties);
        logger.debug("Task Created for "+server.get("displayName"));

        return new OracledbMonitorTask.Builder()
                .metricWriter(serviceProvider.getMetricWriteHelper())
                .metricPrefix(getContextConfiguration().getMetricPrefix())
                .jdbcAdapter(jdbcAdapter)
                .previousTimestamp(previousTimestamp)
                .currentTimestamp(currentTimestamp)
                .server(server).build();

    }
    private String serverName(Map<String, ?> server) {
        String name = Util.convertToString(server.get("displayName"), "");
        return name;
    }

    private String driverName(Map<String, ?> server) {
        String name = Util.convertToString(server.get("driver"), "");
        return name;
    }

    private String createConnectionUrl(Map<String, ?> server) {
        String url = Util.convertToString(server.get("connectionUrl"), "");
        return url;
    }

    private Map<String, String> getConnectionProperties(Map<String, ?> server) {
        Map<String, String> connectionProperties = (Map<String, String>) server.get("connectionProperties");
        String password = connectionProperties.get("password");
        if (Strings.isNullOrEmpty(password))
            password = getPassword(connectionProperties);

        connectionProperties.put("password", password);
        return connectionProperties;
    }

    private String getPassword(Map<String, String> server) {
        String encryptedPassword = server.get(Constant.ENCRYPTED_PASSWORD);
        Map<String, ?> configMap = getContextConfiguration().getConfigYml();
        String encryptionKey = (String) configMap.get(Constant.ENCRYPTION_KEY);
        if (!Strings.isNullOrEmpty(encryptedPassword)) {
            Map<String, String> cryptoMap = Maps.newHashMap();
            cryptoMap.put("encryptedPassword", encryptedPassword);
            cryptoMap.put("encryptionKey", encryptionKey);
            logger.debug("Decrypting the encrypted password........");
            return CryptoUtils.getPassword(cryptoMap);
        }
        return "";
    }

    // private String getEncryptedPassword(String encryptionKey, String encryptedPassword) {
    //     Map<String, String> cryptoMap = Maps.newHashMap();
    //     cryptoMap.put(PASSWORD_ENCRYPTED, encryptedPassword);
    //     cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
    //     return CryptoUtil.getPassword(cryptoMap);
    // }


}
