package org.gbif.checklistbank.cli;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariDataSource;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.importer.DatasetImportServiceCombined;
import org.gbif.checklistbank.cli.importer.Importer;
import org.gbif.checklistbank.cli.importer.ImporterConfiguration;
import org.gbif.checklistbank.cli.importer.ImporterService;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerTest;
import org.gbif.checklistbank.index.NameUsageIndexServicePassThru;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.utils.file.CompressionUtil;

import java.beans.Statement;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.sql.DataSource;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

@Ignore("manual test to discover why we see too many open files in heavy normalizer cli use under linux")
public class NoFileDescriptiorLeakTest {
    private static final ObjectMapper CFG_MAPPER = new ObjectMapper(new YAMLFactory());
    private final int threads = 10;

    private NormalizerConfiguration cfgN;
    private ImporterConfiguration cfgI;
    private File zip;
    private OpenFileMonitor monitor;
    private DatasetImportServiceCombined importService;
    private NameUsageService usageService;
    private HikariDataSource hds;

    public static class OpenFileMonitor extends TimerTask {
        private MBeanServer jmx;
        private ObjectName osMBean;

        public OpenFileMonitor() {
            try {
                osMBean = ObjectName.getInstance("java.lang:type=OperatingSystem");
            } catch (MalformedObjectNameException e) {
                Throwables.propagate(e);
            }
            jmx = getPlatformMBeanServer();
        }

        @Override
        public void run() {
            System.out.println("OPEN FILES: " + getOpenFileDescriptorCount());
        }

        public long getOpenFileDescriptorCount() {
            try {
                return (long) jmx.getAttribute(osMBean, "OpenFileDescriptorCount");
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            return -1;
        }
    }

    @Before
    public void init() throws Exception {
        cfgN = new NormalizerConfiguration();
        cfgN.neo.neoRepository = Files.createTempDirectory("neotest").toFile();
        cfgN.archiveRepository = Files.createTempDirectory("neotestdwca").toFile();

        cfgI = CFG_MAPPER.readValue(Resources.getResource("cfg-importer.yaml"), ImporterConfiguration.class);
        cfgI.neo = cfgN.neo;
        System.out.println("Using postgres instance" + cfgI.clb.serverName + " " + cfgI.clb.databaseName);

        URL resourceUrl = getClass().getResource("/plazi.zip");
        Path plazi = Paths.get(resourceUrl.toURI());
        Path zip = Files.createTempFile("dwca", ".zip");
        Files.copy(plazi, zip, StandardCopyOption.REPLACE_EXISTING);
        this.zip = zip.toFile();
        System.out.println("Copied zip resource to tmp file " + zip);
        this.zip = new File("/Users/markus/code/checklistbank/checklistbank-cli/src/test/resources/plazi.zip");

        Timer timer = new Timer();
        monitor = new OpenFileMonitor();
        timer.schedule(monitor, 1000);
    }

    @After
    public void cleanup() {
        FileUtils.deleteQuietly(cfgN.neo.neoRepository);
        FileUtils.deleteQuietly(cfgN.archiveRepository);
        if (hds != null) {
            hds.close();
        }
    }

    @Test
    public void manyNormalizersInParallel() throws Exception {
        final int tasks = 500;

        ExecutorCompletionService<Object> ecs = new ExecutorCompletionService(Executors.newFixedThreadPool(threads));
        List<Future<Object>> futures = Lists.newArrayList();

        for (int i = 0; i < tasks; i++) {
            UUID dk = UUID.randomUUID();

            // copy dwca
            File dwca = cfgN.archiveDir(dk);
            CompressionUtil.decompressFile(dwca, this.zip);

            Normalizer normalizer = NormalizerTest.buildNormalizer(cfgN, dk);
            System.out.println("Submit normalizer " + i++ + " with open files: " + monitor.getOpenFileDescriptorCount());
            futures.add(ecs.submit(Executors.callable(normalizer)));
        }

        int idx = 1;
        for (Future<Object> f : futures) {
            f.get();
            System.out.println("Finished normalizer " + idx++ + " with open files: " + monitor.getOpenFileDescriptorCount());
        }
        System.out.println("Finished all threads");
    }

    @Test
    public void manyImporterInParallel() throws Exception {
        final int tasks = 100;

        // init mybatis layer and solr from cfgN instance
        Injector inj = Guice.createInjector(cfgI.clb.createServiceModule());
        importService = new DatasetImportServiceCombined(inj.getInstance(DatasetImportService.class), new NameUsageIndexServicePassThru());
        usageService = inj.getInstance(NameUsageService.class);

        Key<DataSource> dsKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
        hds = (HikariDataSource) inj.getInstance(dsKey);
        // truncate tables
        System.out.println("Truncate existing data");
        Connection cn = hds.getConnection();
        java.sql.Statement st = cn.createStatement();
        st.execute("truncate name_usage cascade");
        st.execute("truncate name cascade");
        st.execute("truncate citation cascade");
        st.close();
        cn.close();

        ExecutorCompletionService<UUID> ecs = new ExecutorCompletionService(Executors.<UUID>newFixedThreadPool(threads));
        LinkedList<Future<UUID>> futures = Lists.newLinkedList();

        System.out.println("Start creating normalization tasks");
        for (int i = 0; i < tasks; i++) {
            UUID dk = UUID.randomUUID();

            // copy dwca
            File dwca = cfgN.archiveDir(dk);
            CompressionUtil.decompressFile(dwca, this.zip);

            Normalizer normalizer = NormalizerTest.buildNormalizer(cfgN, dk);
            futures.add(
                    ecs.submit(Executors.callable(normalizer, dk))
            );
        }

        while(!futures.isEmpty()) {
            Future<UUID> f = futures.pop();
            UUID dk = f.get();
            if (dk != null) {
                // this was a normalizer, submit an importer
                System.out.println("Finished normalizer " + dk + " with open files: " + monitor.getOpenFileDescriptorCount());
                futures.addFirst(
                        ecs.submit(Executors.callable(build(dk), null))
                );
            } else {
                System.out.println("Finished importer with open files: " + monitor.getOpenFileDescriptorCount());
            }
        }
        System.out.println("Finished all tasks. Done");
    }

    public Importer build(UUID datasetKey) throws SQLException {
        MetricRegistry registry = new MetricRegistry("normalizer");
        MemoryUsageGaugeSet mgs = new MemoryUsageGaugeSet();
        registry.registerAll(mgs);
        registry.meter(ImporterService.SYNC_METER);
        return new Importer(cfgI, datasetKey, registry, importService, usageService);
    }

}
