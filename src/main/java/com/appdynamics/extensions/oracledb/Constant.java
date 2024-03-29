package com.appdynamics.extensions.oracledb;

public class Constant {
    public static String USER_NAME;
    public static String PASSWORD;
    public static String ENCRYPTED_PASSWORD;
    public static String ENCRYPTION_KEY;
    public static String METRIC_SEPARATOR;
    public static String METRIC_PREFIX;
    public static String MonitorName;

    static {
        METRIC_PREFIX = "Custom Metrics|OracleDB";
        MonitorName = "OracleDB Monitor";
        USER_NAME = "username";
        ENCRYPTED_PASSWORD = "encryptedPassword";
        ENCRYPTION_KEY = "encryptionKey";
        PASSWORD = "password";
        METRIC_SEPARATOR = "|";
    }
}