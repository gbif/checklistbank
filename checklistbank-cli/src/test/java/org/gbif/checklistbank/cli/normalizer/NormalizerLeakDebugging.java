/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.normalizer;

import org.gbif.nub.config.ClbNubConfiguration;
import org.gbif.nub.lookup.straight.IdLookupImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;

/**
 * Main integration tests for the normalizer testing imports of entire small checklists.
 */
@Disabled
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
//    Properties properties = PropertiesUtil.loadProperties(ClbLoadTestDb.DEFAULT_PROPERTY_FILE);
//    ChecklistBankServiceMyBatisModule module = new ChecklistBankServiceMyBatisModule(properties);
//    cfg.clb=module.provideCfg();

    System.out.println("Load nublookup into memory");
    lookup.load(ClbNubConfiguration.fromClbConfiguration(cfg.clb), false);

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