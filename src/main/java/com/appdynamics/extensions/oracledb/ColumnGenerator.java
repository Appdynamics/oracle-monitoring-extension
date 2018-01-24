package com.appdynamics.extensions.oracledb;

import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Created by bhuvnesh.kumar on 10/5/17.
 */
public class ColumnGenerator {

    public List<Column> getColumns(Map query) {
        AssertUtils.assertNotNull(query.get("columns"),"Queries need to have columns configured.");

        Map<String, Map<String, String>> filter = Maps.newLinkedHashMap();
        filter = filterMap(query, "columns");
        final ObjectMapper mapper = new ObjectMapper(); // jackson’s objectmapper
        final Columns columns = mapper.convertValue(filter, Columns.class);
        return columns.getColumns();
    }

    private Map<String, Map<String, String>> filterMap( Map<String, Map<String, String>> mapOfMaps, String filterKey) {
        Map<String, Map<String, String>> filteredOnKeyMap = Maps.newLinkedHashMap();

        if (Strings.isNullOrEmpty(filterKey))
            return filteredOnKeyMap;

        if (mapOfMaps.containsKey(filterKey)) {
            filteredOnKeyMap.put(filterKey,mapOfMaps.get(filterKey));
        }

        return filteredOnKeyMap;
    }
}
