package com.appdynamics.extensions.oracledb;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by bhuvnesh.kumar on 9/21/17.
 */
public class Columns {
    List<Column> columns = Lists.newArrayList();

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }
}
