package org.gbif.checklistbank.cli.normalizer;

import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

@Ignore("manual test to discover why we see too many open files in heavy normalizer cli use under linux")
public class NoFileDescriptiorLeakTest {
  private NormalizerConfiguration cfg;
  private File zip;

  @Test
  public void manyNormalizersInParallel() throws Exception {
    final int parallelNormalizer = 100;

    cfg = new NormalizerConfiguration();
    cfg.neo.neoRepository = Files.createTempDirectory("neotest").toFile();
    cfg.archiveRepository = Files.createTempDirectory("neotestdwca").toFile();

    URL resourceUrl = getClass().getResource("/plazi.zip");
    Path plazi = Paths.get(resourceUrl.toURI());
    Path zip = Files.createTempFile("dwca", ".zip");
    Files.copy(plazi, zip, StandardCopyOption.REPLACE_EXISTING);
    this.zip = zip.toFile();
    System.out.println("Copied zip resource to tmp file " + zip);
    //this.zip = new File("/Users/markus/code/checklistbank/checklistbank-cli/src/test/resources/plazi.zip");

    Timer timer = new Timer();
    OpenFileMonitor monitor = new OpenFileMonitor();
    timer.schedule(monitor , 2000 );

    ExecutorService executor = Executors.newFixedThreadPool(5);
    ExecutorCompletionService<Object> ecs = new ExecutorCompletionService(executor);
    List<Future<Object>> futures = Lists.newArrayList();

    for (int i=0; i < parallelNormalizer; i++) {
      UUID dk = UUID.randomUUID();

      // copy dwca
      File dwca = cfg.archiveDir(dk);
      CompressionUtil.decompressFile(dwca, this.zip);

      Normalizer normalizer = NormalizerTest.buildNormalizer(cfg, dk);
      futures.add(ecs.submit(Executors.callable(normalizer)));
    }

    int idx = 1;
    for (Future<Object> f : futures) {
      f.get();
      System.out.println("Finished normalizer " + idx++ + " with open files: " + monitor.getOpenFileDescriptorCount());
    }
    System.out.println("Finished all threads");

    FileUtils.deleteQuietly(cfg.neo.neoRepository);
    FileUtils.deleteQuietly(cfg.archiveRepository);
  }

  class OpenFileMonitor extends TimerTask {
    private MBeanServer jmx;
    private ObjectName osMBean;

    public OpenFileMonitor() throws MalformedObjectNameException {
      osMBean = ObjectName.getInstance("java.lang:type=OperatingSystem");
      jmx = getPlatformMBeanServer();
    }

    @Override
    public void run() {
      System.out.println("OPEN FILES: " + getOpenFileDescriptorCount());
    }

    public long getOpenFileDescriptorCount() {
      try {
        return (long) jmx.getAttribute( osMBean, "OpenFileDescriptorCount" );
      } catch (Exception e) {
        Throwables.propagate(e);
      }
      return -1;
    }
  }
}
