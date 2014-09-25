package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.crawler.NormalizerStats;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Manual test to insert a neo db that lasts so we can visualize it with the neo server
 */
@Ignore
public class CreateNeoDb {

  /**
   */
  @Test
  public void createNeoDb() throws Exception {
    NormalizerConfiguration cfg = new NormalizerConfiguration();
    File tmp = new File("/Users/markus/neodbs");
    cfg.neo.neoRepository = tmp;
    URL dwcasUrl = getClass().getResource("/dwcas");
    Path p = Paths.get(dwcasUrl.toURI());
    cfg.archiveRepository = p.toFile();

    Normalizer norm = NormalizerTest.buildNormalizer(cfg, NormalizerTest.datasetKey(9));
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);
  }
}
