package org.gbif.checklistbank.cli.admin;

import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.common.search.solr.SolrConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.google.common.collect.Lists;
import org.apache.commons.io.LineIterator;

/**
 *
 */
public class AdminConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public SolrConfig dataset = new SolrConfig();

  @ParametersDelegate
  @Valid
  @NotNull
  public ClbConfiguration clb = new ClbConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public ZooKeeperConfiguration zookeeper = new ZooKeeperConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public NeoConfiguration neo;

  @Parameter(names = "--archive-repository")
  @NotNull
  public File archiveRepository;

  @Parameter(names = "--export-repository")
  @NotNull
  public File exportRepository = new File("./exports");

  @Parameter(names = {"-k", "--key"}, required = false)
  @Nullable
  public UUID key;

  @Parameter(names = {"-f", "--keys"}, required = false)
  @Nullable
  public File keys;

  @Parameter(names = {"-t", "--type"}, required = false)
  @NotNull
  public DatasetType type = DatasetType.CHECKLIST;

  @Parameter(names = {"-op", "--operation"}, required = true)
  @NotNull
  public AdminOperation operation;

  @Valid
  @Parameter(names = {"--nub-ranks-only"}, required = false)
  public boolean nubRanksOnly = false;

  /**
   * Do not change any information in checklistbank, just report potential changes, e.g. for reparsed names
   */
  @Parameter(names = {"--dry-run"}, required = false)
  public boolean dryRun = false;

  /**
   * If true sets the Backbone dataset key in the key config
   */
  @Parameter(names = {"--nub"}, required = false)
  public boolean nub = false;

  /**
   * If true sets the Catalog of Life dataset key in the key config
   */
  @Parameter(names = {"--col"}, required = false)
  public boolean col = false;

  /**
   * If true sets the Plazi publisher key in the key config
   */
  @Parameter(names = {"--plazi"}, required = false)
  public boolean plazi = false;

  /**
   * Returns the directory with the decompressed archive folder created by the dwca downloader.
   */
  public File archiveDir(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString());
  }

  public List<UUID> listKeys() {
    List<UUID> result = Lists.newArrayList();
    if (keys != null || keys.exists()) {
      try {
        LineIterator lines = new LineIterator(new FileReader(keys));
        while (lines.hasNext()) {
          try {
            result.add(UUID.fromString(lines.nextLine()));
          } catch (IllegalArgumentException e) {
            // ignore
          }
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    return result;
  }
}
