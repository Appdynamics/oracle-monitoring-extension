/*
 * Copyright 2013. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.oracle.config;

import com.appdynamics.extensions.crypto.CryptoUtil;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.appdynamics.TaskInputArgs.ENCRYPTION_KEY;
import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;

public class Configuration {

    private String host;
    private int port;
    private String sid;
    private String username;
    private String password;
    private String encryptedPassword;
    private String encryptionKey;

    private String metricPathPrefix;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        if(encryptionKey != null){
            Map cryptoMap = Maps.newHashMap();
            cryptoMap.put(PASSWORD_ENCRYPTED,encryptedPassword);
            cryptoMap.put(ENCRYPTION_KEY,encryptionKey);
            password = CryptoUtil.getPassword(cryptoMap);
        }
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMetricPathPrefix() {
        return metricPathPrefix;
    }

    public void setMetricPathPrefix(String metricPathPrefix) {
        this.metricPathPrefix = metricPathPrefix;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
