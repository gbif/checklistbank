package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.DatasetAnalysisService;

import java.util.Date;
import java.util.UUID;

import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetAnalysisServiceMyBatisIT extends MyBatisServiceITBase<DatasetAnalysisService> {

    public DatasetAnalysisServiceMyBatisIT() {
        super(DatasetAnalysisService.class);
    }

    @Test
    public void testAnalyse() {
        final Date downloaded = new Date();
        DatasetMetrics d = service.analyse(ClbDbTestRule.SQUIRRELS_DATASET_KEY, downloaded);
        System.out.println(d);
        assertEquals(ClbDbTestRule.SQUIRRELS_DATASET_KEY, d.getDatasetKey());
        assertEquals(downloaded, d.getDownloaded());
        assertEquals(44, d.getUsagesCount());
        assertEquals(16, d.getSynonymsCount());
        assertEquals(44, d.getDistinctNamesCount());
        assertEquals(2, d.getNubMatchingCount());
        assertEquals(4, d.getNubCoveragePct());
        assertEquals(0, d.getColCoveragePct());
        assertEquals(0, d.getColMatchingCount());
        assertEquals(1, d.getCountByKingdom().size());
        // there are more animal records in this dataset, but only 2 are mapped to the nub!
        assertEquals(2, d.getCountByKingdom(Kingdom.ANIMALIA));
        assertEquals(0, d.getCountByKingdom(Kingdom.PLANTAE));
        assertEquals(10, d.getCountByRank(Rank.SUBSPECIES));
        assertEquals(3, d.getCountByRank(Rank.SPECIES));
        assertEquals(2, d.getCountByRank(Rank.GENUS));
        assertEquals(1, d.getCountByRank(Rank.PHYLUM));
        assertEquals(4, d.getCountNamesByLanguage(Language.ENGLISH));
        assertEquals(2, d.getCountNamesByLanguage(Language.GERMAN));
    }

}