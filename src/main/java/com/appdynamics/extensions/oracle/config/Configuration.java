/**
 * Copyright 2015 AppDynamics
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
