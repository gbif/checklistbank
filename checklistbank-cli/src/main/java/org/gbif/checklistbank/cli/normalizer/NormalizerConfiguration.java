package org.gbif.checklistbank.cli.normalizer;

import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.MessagingConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;

import java.io.File;
import java.util.Properties;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@SuppressWarnings("PublicField")
public class NormalizerConfiguration {

  @ParametersDelegate
  @Valid
  @NotNull
  public GangliaConfiguration ganglia = new GangliaConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public NeoConfiguration neo = new NeoConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(names = "--archive-repository")
  @NotNull
  public File archiveRepository;

  @ParametersDelegate
  @Valid
  @NotNull
  public String matchWsUrl = "http://api.gbif.org/v1/species/match";

  @ParametersDelegate
  @Valid
  @NotNull
  public ZooKeeperConfiguration zookeeper = new ZooKeeperConfiguration();

  public ChecklistBankWsClientModule createMatchClientModule() {
    Properties props = new Properties();
    props.put("checklistbank.match.ws.url", matchWsUrl);
    return new ChecklistBankWsClientModule(props, false, true);
  }

  /**
   * Returns the directory with the decompressed archive folder created by the dwca downloader.
   */
  public File archiveDir(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString());
  }

}
