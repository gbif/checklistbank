package org.gbif.checklistbank.cli.normalizer;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.junit.Ignore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Main integration tests for the normalizer testing imports of entire small checklists.
 */
@Ignore
public class NormalizerLeakDebugging {
  static final String DWCA_RESOURCE = "/dwcas/00000001-c6af-11e2-9b88-00145eb45e9a";
  static final String[] FILES = new String[]{"taxa.txt"};

  NormalizerConfiguration cfg;
  ExecutorService exec;
  AtomicInteger counter = new AtomicInteger();
  IdLookupImpl lookup = IdLookupImpl.temp();
  MetricRegistry registry = new MetricRegistry();

  private void init() throws Exception {
    System.out.println("Setup configs");
    cfg = new NormalizerConfiguration();
    cfg.neo.neoRepository = new File("/tmp/clb/neo");
    cfg.neo.neoRepository.mkdirs();

    cfg.archiveRepository = new File("/tmp/clb/dwca");
    cfg.archiveRepository.mkdirs();
    Properties properties = PropertiesUtil.loadProperties(ClbDbTestRule.DEFAULT_PROPERTY_FILE);
    ChecklistBankServiceMyBatisModule module = new ChecklistBankServiceMyBatisModule(properties);
    cfg.clb=module.provideCfg();

    System.out.println("Load nublookup into memory");
    lookup.load(cfg.clb, false);

    FileUtils.cleanDirectory(cfg.neo.neoRepository);
    FileUtils.cleanDirectory(cfg.archiveRepository);
  }

  public void run(int threads) throws Exception {
    init();
    exec = Executors.newFixedThreadPool(threads);
    for (int x=0; x< threads*2; x++) {
      exec.submit(new RunNormalizer());
      TimeUnit.SECONDS.sleep(2);
    }
  }

  class RunNormalizer implements Callable<UUID> {
    final UUID datasetKey = UUID.randomUUID();

    @Override
    public UUID call() throws Exception {
      System.out.println("Start " + datasetKey);
      try {
        // copy dwca
        File dir = cfg.archiveDir(datasetKey);
        dir.mkdirs();
        System.out.println("Copy resource to " + dir);
        for (String fn : FILES) {
          File target = new File(dir, fn);
          try (Writer w = new FileWriter(target)) {
            IOUtils.copy(getClass().getResourceAsStream(DWCA_RESOURCE+"/"+fn), w, StandardCharsets.UTF_8);
          }
        }
        // run normalizer
        Normalizer norm = Normalizer.create(cfg, datasetKey, registry, Maps.newHashMap(), lookup);
        norm.run();

        // count total jobs
        System.out.println("Finished " + datasetKey);
        System.out.println("Total jobs completed: " + counter.incrementAndGet());

        // submit new job, we dont ever wanna stop!
        exec.submit(new RunNormalizer());

      } catch (Exception e) {
        e.printStackTrace();
      }

      return datasetKey;
    }
  }

  public static void main(String[] args) throws Exception {
    NormalizerLeakDebugging debugger = new NormalizerLeakDebugging();
    debugger.run(4);
  }

}