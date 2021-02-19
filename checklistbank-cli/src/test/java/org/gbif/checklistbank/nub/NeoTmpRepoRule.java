package org.gbif.checklistbank.nub;

import org.apache.commons.io.FileUtils;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;

public class NeoTmpRepoRule implements TestRule {
    public final NeoConfiguration cfg;

    public NeoTmpRepoRule() {
        this.cfg = new NeoConfiguration();
        cfg.neoRepository = new File(FileUtils.getTempDirectory(), "clb");
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    private void before() throws IOException {
        if (cfg.neoRepository.exists()) {
            FileUtils.cleanDirectory(cfg.neoRepository);
        } else {
            cfg.neoRepository.mkdirs();
        }
    }

    private void after() {
        FileUtils.deleteQuietly(cfg.neoRepository);
    }
}
