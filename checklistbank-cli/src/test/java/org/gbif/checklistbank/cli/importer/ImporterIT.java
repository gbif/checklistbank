package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.cli.normalizer.NeoTest;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerIT;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class ImporterIT extends NeoTest {

  private static final String INCERTAE_SEDIS = "Incertae sedis";
  private NormalizerConfiguration nCfg;
  private ImporterConfiguration iCfg;

  @Before
  public void initDwcaRepo() throws Exception {
    nCfg = new NormalizerConfiguration();
    nCfg.neo = super.cfg;

    URL dwcasUrl = getClass().getResource("/dwcas");
    Path p = Paths.get(dwcasUrl.toURI());
    nCfg.archiveRepository = p.toFile();

    iCfg = new ImporterConfiguration();
    iCfg.neo = nCfg.neo;
    iCfg.clb.url="jdbc:postgresql://localhost/clbtest";
    iCfg.clb.username="postgres";
    iCfg.clb.password="pogo";
  }

  @Test
  public void testIdList() {
    final UUID datasetKey = NormalizerIT.datasetKey(1);

    // create neo db //TODO: once stable keep neo db as test resource?
    Normalizer norm = Normalizer.build(nCfg, datasetKey, null);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    // import
    Importer importer = Importer.build(iCfg, datasetKey);
    importer.run();
  }
}