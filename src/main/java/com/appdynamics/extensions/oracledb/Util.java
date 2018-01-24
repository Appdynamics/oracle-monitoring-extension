package com.appdynamics.extensions.oracledb;


public class Util {


    static String convertToString(final Object field,final String defaultStr){
        if(field == null){
            return defaultStr;
        }
        return field.toString();
    }


}
