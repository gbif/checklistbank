package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetMetricsServiceMyBatisIT extends MyBatisServiceITBase<DatasetMetricsService> {

    public DatasetMetricsServiceMyBatisIT() {
        super(DatasetMetricsService.class);
    }

    @Test
    public void testInsert() {
        DatasetMetricsServiceMyBatis srv = (DatasetMetricsServiceMyBatis) service;

        srv.create(ClbDbTestRule.SQUIRRELS_DATASET_KEY, new Date());

        DatasetMetrics d = service.get(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
        assertEquals(ClbDbTestRule.SQUIRRELS_DATASET_KEY, d.getDatasetKey());
        assertEquals(44, d.getUsagesCount());
        assertEquals(16, d.getSynonymsCount());
        assertEquals(44, d.getDistinctNamesCount());
        assertEquals(2, d.getNubMatchingCount());
        assertEquals(0, d.getColMatchingCount());
        assertEquals(0, d.getColCoveragePct());

        assertEquals(44, d.getCountByOrigin(Origin.SOURCE));
        assertEquals(2, d.getCountByKingdom(Kingdom.ANIMALIA));
        assertEquals(0, d.getCountByKingdom(Kingdom.FUNGI));
        assertEquals(2, d.getCountByRank(Rank.GENUS));
        assertEquals(1, d.getCountByRank(Rank.PHYLUM));
        assertEquals(10, d.getCountByRank(Rank.SUBSPECIES));
        assertEquals(1, d.getCountByRank(Rank.FAMILY));
        assertEquals(0, d.getCountNamesByLanguage(Language.DANISH));
        assertEquals(2, d.getCountNamesByLanguage(Language.GERMAN));
        assertEquals(1, d.getCountByIssue(NameUsageIssue.RANK_INVALID));
        assertEquals(0, d.getCountByIssue(NameUsageIssue.BACKBONE_MATCH_NONE));
        assertEquals(0, d.getCountByIssue(NameUsageIssue.CLASSIFICATION_NOT_APPLIED));
    }

    @Test
    public void testGet() {
        DatasetMetrics d = service.get(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
        assertEquals(ClbDbTestRule.SQUIRRELS_DATASET_KEY, d.getDatasetKey());
        assertEquals(1000, d.getUsagesCount());
        assertEquals(25, d.getColCoveragePct());
        assertEquals(250, d.getColMatchingCount());
        assertEquals(100, d.getCountByKingdom(Kingdom.ANIMALIA));
        assertEquals(700, d.getCountByKingdom(Kingdom.PLANTAE));
        assertEquals(0, d.getCountByKingdom(Kingdom.FUNGI));
        assertEquals(120, d.getCountByRank(Rank.GENUS));
        assertEquals(10, d.getCountByRank(Rank.PHYLUM));
        assertEquals(4, d.getCountNamesByLanguage(Language.DANISH));
        assertEquals(132, d.getCountNamesByLanguage(Language.GERMAN));
    }

    @Test
    public void testList() {
        List<DatasetMetrics> ds = service.list(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
        assertEquals(3, ds.size());
        for (DatasetMetrics d : ds) {
            assertEquals(ClbDbTestRule.SQUIRRELS_DATASET_KEY, d.getDatasetKey());
        }
        assertEquals(1000, ds.get(0).getUsagesCount());
        assertEquals(200, ds.get(1).getUsagesCount());
        assertEquals(100, ds.get(2).getUsagesCount());
    }

}
