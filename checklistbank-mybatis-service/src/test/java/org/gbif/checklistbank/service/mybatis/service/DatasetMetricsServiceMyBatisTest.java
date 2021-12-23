package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.service.mybatis.service.DatasetMetricsServiceMyBatis;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetMetricsServiceMyBatisTest {

    @Test
    public void testPercentage() {
        assertEquals(9, DatasetMetricsServiceMyBatis.getPercentage(103, 1078));
        assertEquals(9, DatasetMetricsServiceMyBatis.getPercentage(90, 1000));
        assertEquals(0, DatasetMetricsServiceMyBatis.getPercentage(1, 1078));
        assertEquals(0, DatasetMetricsServiceMyBatis.getPercentage(0, 1078));
        assertEquals(99, DatasetMetricsServiceMyBatis.getPercentage(1077, 1078));
        assertEquals(100, DatasetMetricsServiceMyBatis.getPercentage(1077, 1077));
    }

    @Test
    public void testCount() {
        List<DatasetMetricsServiceMyBatis.Count<String>> counts = Lists.newArrayList();

        counts.add(new DatasetMetricsServiceMyBatis.Count<String>("c", 19));
        counts.add(new DatasetMetricsServiceMyBatis.Count<String>("d", 9));
        counts.add(new DatasetMetricsServiceMyBatis.Count<String>("b", 229));
        counts.add(new DatasetMetricsServiceMyBatis.Count<String>("few", 3214129));
        counts.add(new DatasetMetricsServiceMyBatis.Count<String>("a", -4));
        counts.add(new DatasetMetricsServiceMyBatis.Count<String>("g", -4));

        Collections.sort(counts);

        assertEquals("g", counts.get(5).getKey());
        assertEquals("a", counts.get(4).getKey());
        assertEquals("d", counts.get(3).getKey());
        assertEquals("c", counts.get(2).getKey());
        assertEquals("b", counts.get(1).getKey());
        assertEquals("few", counts.get(0).getKey());
    }

}
