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
package org.gbif.checklistbank.neo;

import org.gbif.checklistbank.cli.normalizer.InsertMetadata;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.utils.ResourcesMonitor;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;

@Disabled("manual long running test to discover why we see too many hanging PageCache threads in neos batch inserter")
public class MultiThreadingNeoInserterTest {
    private final int threads = 5;

    public class InserterJob implements Callable<InsertMetadata> {
        private final UUID key;
        private final NormalizerConfiguration cfg;
        private final MetricRegistry registry;

        public InserterJob(UUID key, NormalizerConfiguration cfg, MetricRegistry registry) {
            this.cfg = cfg;
            this.key = key;
            this.registry = registry;
        }

        @Override
        public InsertMetadata call() throws Exception {
            UsageDao dao = UsageDao.persistentDao(cfg.neo, key, registry, true);
            try {
                NeoInserter ins = dao.createBatchInserter(100);
                File dwca = cfg.archiveDir(key);
                InsertMetadata m = ins.insert(dwca, Maps.<String, UUID>newHashMap());
                ins.close();
                return m;
            } finally {
                dao.close();
            }
        }
    }

    @Test
    public void manyNormalizersInParallel() throws Exception {
        final int tasks = 500;

        NormalizerConfiguration cfgN = new NormalizerConfiguration();
        cfgN.neo.neoRepository = Files.createTempDirectory("neotest").toFile();
        cfgN.archiveRepository = Files.createTempDirectory("neotestdwca").toFile();

        MetricRegistry registry = new MetricRegistry();

        File zip = new File(getClass().getResource("/plazi.zip").getFile());
        zip = new File("/Users/markus/code/checklistbank/checklistbank-cli/src/test/resources/plazi.zip");

        Timer timer = new Timer();
        ResourcesMonitor monitor = new ResourcesMonitor();
        timer.schedule(monitor, 500);


        ExecutorCompletionService<InsertMetadata> ecs = new ExecutorCompletionService(Executors.newFixedThreadPool(threads));
        List<Future<InsertMetadata>> futures = Lists.newArrayList();
        for (int i = 0; i < tasks; i++) {
            UUID dk = UUID.randomUUID();

            // copy dwca
            File dwca = cfgN.archiveDir(dk);
            CompressionUtil.decompressFile(dwca, zip);

            System.out.println("Submit inserter job " + i);
            futures.add(ecs.submit(new InserterJob(dk, cfgN, registry)));
        }

        int idx = 1;
        for (Future<InsertMetadata> f : futures) {
            f.get();
            System.out.println("Finished inserter " + idx++);
            monitor.run();
        }
        System.out.println("Finished all jobs");
        System.out.println("Open files: " + monitor.getOpenFileDescriptorCount());
        System.out.println("Running threads: " + Thread.getAllStackTraces().size());
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            System.out.println(t.getState() + " " + t.getName());
        }

        System.out.println("Cleaning up artifacts...");
        FileUtils.deleteQuietly(cfgN.neo.neoRepository);
        FileUtils.deleteQuietly(cfgN.archiveRepository);
    }

}
