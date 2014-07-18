package org.gbif.checklistbank.cli.normalizer;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manual test to create a neo db that lasts so we can visualize it with the neo server
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

        Normalizer norm = Normalizer.build(cfg, NormalizerIT.datasetKey(9), null);
        NormalizerStats stats = norm.run();
        System.out.println(stats);
    }
}
