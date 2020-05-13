/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.oracledb;


import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.google.common.base.Strings;

import java.sql.*;
import java.util.Map;
import java.util.Properties;


public class JDBCConnectionAdapter {

    private final String connUrl;
    private final Map<String, String> connectionProperties;
    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(JDBCConnectionAdapter.class);


    private JDBCConnectionAdapter(String connStr, Map<String, String> connectionProperties) {
        this.connUrl = connStr;
        this.connectionProperties = connectionProperties;

    }

    static JDBCConnectionAdapter create(String connUrl, Map<String, String> connectionProperties) {
        return new JDBCConnectionAdapter(connUrl, connectionProperties);
    }

    Connection open(String driver) throws SQLException, ClassNotFoundException {
        Connection connection;
        Class.forName(driver);

        Properties properties = new Properties();

        if (connectionProperties != null) {
            for (String key : connectionProperties.keySet()) {
                if (!Strings.isNullOrEmpty(connectionProperties.get(key)))
                    properties.put(key, connectionProperties.get(key));
            }
        }
        logger.debug("Passed all checks for properties and attempting to connect to: "+ connUrl);
        long timestamp1 = System.currentTimeMillis();

        connection = DriverManager.getConnection(connUrl, properties.getProperty("user"), properties.getProperty("password"));
        long timestamp2 = System.currentTimeMillis();
        logger.debug("Connection received in JDBC ConnectionAdapter in :"+ (timestamp2-timestamp1)+ " ms");
        return connection;
    }

    ResultSet queryDatabase(String query, Statement stmt) throws SQLException {
        return stmt.executeQuery(query);
    }

    void closeStatement(Statement statement) throws SQLException {
        statement.close();
    }

    void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }
}
