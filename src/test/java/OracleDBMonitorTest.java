/*
 * Copyright 2013. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */


import com.appdynamics.extensions.oracle.OracleDbMonitor;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OracleDBMonitorTest {
    public static final String CONFIG_ARG = "config-file";

  /*  @Test
    public void testOracleDBMonitor() throws TaskExecutionException, ClassNotFoundException {
        OracleDbMonitor monitor = new OracleDbMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml");
        monitor.execute(taskArgs, null);
    }*/


    @Test
    public void testMultiMaps(){
        Multimap<String,String> multiMap = ArrayListMultimap.create();
        multiMap.put("SampleA","300");
        multiMap.put("SampleA","0");
        multiMap.put("SampleB","200");
        multiMap.put("SampleB","1");
        multiMap.put("SampleC","200");
        multiMap.put("SampleC","0");
        multiMap.put("SampleD","500");
        multiMap.put("SampleD","1");

        List<String> values = (List<String>) multiMap.get("SampleA");
        Assert.assertTrue(values != null && values.get(0) != null);

    }


}
