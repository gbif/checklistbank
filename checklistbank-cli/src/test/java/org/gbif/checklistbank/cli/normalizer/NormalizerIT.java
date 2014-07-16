package org.gbif.checklistbank.cli.normalizer;

import org.gbif.utils.file.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class NormalizerIT {
    private NormalizerConfiguration cfg;

    @Before
    public void initCfg() throws Exception {
        cfg = new NormalizerConfiguration();
        URL dwcasUrl = getClass().getResource("/dwcas");
        Path p = Paths.get(dwcasUrl.toURI());
        cfg.archiveRepository = p.toFile();
        File tmp = FileUtils.createTempDir();
        cfg.neo.neoRepository = tmp;
    }

    @After
    public void cleanup() throws Exception {
        // org.apache.commons.io.FileUtils.cleanDirectory(cfg.neo.neoRepository);
    }

    @Test
    public void testRun() throws Exception {
        Normalizer norm = Normalizer.build(cfg, UUID.fromString("8ea44a78-c6af-11e2-9b88-00145eb45e9a"));
        norm.run();
    }

}