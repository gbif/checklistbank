package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.UsageService;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UsageServiceMyBatisTest extends MyBatisServiceITBase<UsageService> {

    public UsageServiceMyBatisTest() {
        super(UsageService.class);
    }

    @Test
    public void testlistAll() {
        List<Integer> squirrels = service.listAll();
        assertEquals(46, squirrels.size());
    }

    @Test
    public void testlistParents() {
        List<Integer> squirrels = service.listParents(100000007);
        assertEquals(8, squirrels.size());
    }
}