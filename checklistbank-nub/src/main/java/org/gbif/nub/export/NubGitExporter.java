package org.gbif.nub.export;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental export of the nub or any other dataset as README and json files in a hierarchical filestructure that
 * can be browsed in github nicely.
 */
public class NubGitExporter {
  private static final Logger LOG = LoggerFactory.getLogger(NubGitExporter.class);
  private static final Charset UTF8 = Charset.forName("UTF8");

  private int PAGE_SIZE = 10000;
  private final String ROOTDIR = "/Users/mdoering/dev/backbone/life";
  private final Joiner pathJoiner = Joiner.on(File.separator).skipNulls();
  private final String readmeTempl;
  private final UsageService usageService;
  private ObjectMapper mapper = new ObjectMapper();

  @Inject
  public NubGitExporter(UsageService usageService) throws IOException {
    this.usageService = usageService;
    readmeTempl = Resources.toString(Resources.getResource("exporter_readme.txt"), UTF8);
    mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
  }

  public void run() {
    final int max = usageService.maxUsageKey(Constants.NUB_DATASET_KEY) + 1;
    int start = 0;
    LOG.info("Retrieved max backbone usage key {}", max);
    while (start < max) {
      int end = start + PAGE_SIZE - 1;
      List<NameUsage> usages = usageService.listRange(start, end);
      LOG.info("Retrieved {} usages from {}-{}", usages.size(), start, end);
      for (NameUsage u : usages) {
        dumpUsage(u);
      }
      start += PAGE_SIZE;
    }
  }

  private void dumpUsage(NameUsage u) {
    try {
      StringBuilder name = new StringBuilder();
      if (u.isSynonym()) {
        name.append(" Syn. ");
      }
//      name.append(u.getRank().ordinal());
//      name.append(u.getRank());
//      name.append(" ");
      name.append(u.getCanonicalName());

      File uDir = new File(pathJoiner.join(ROOTDIR, pathJoiner.join(u.getHigherClassificationMap().values()), name.toString()));
      File readme = new File(uDir, "README.md");
      File json = new File(uDir, "data.json");
      // create folders if missing
      Files.createParentDirs(readme);
      // write readme
      Files.write(String.format(readmeTempl, u.getScientificName(), u.getRank(), u.getTaxonomicStatus(), u.getAccordingTo(), u.getPublishedIn(), u.getBasionym(), u.getRemarks()), readme, UTF8);
      // write json
      mapper.writeValue(json, u);

    } catch (IOException e) {
      LOG.error("Failed to write usage {}: {}", u.getKey(), u.getScientificName(), e);
    }
  }

  public static void main(String[] args) throws IOException {
    Injector inj = Guice.createInjector(new ExporterModule(PropertiesUtil.loadProperties("nub.properties")));
    NubGitExporter exp = inj.getInstance(NubGitExporter.class);
    exp.run();
  }
}
