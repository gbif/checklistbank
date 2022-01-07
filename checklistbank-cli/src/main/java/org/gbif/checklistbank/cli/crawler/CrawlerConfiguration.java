package org.gbif.checklistbank.cli.crawler;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.checklistbank.config.RegistryServiceConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.UUID;

@SuppressWarnings("PublicField")
public class CrawlerConfiguration {

  @ParametersDelegate
  @Valid
  @NotNull
  public GangliaConfiguration ganglia = new GangliaConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  @Parameter(names = "--archive-repository")
  @NotNull
  public File archiveRepository;

  @Parameter(names = "--pool-size")
  @Min(1)
  public int poolSize = 1;

  /**
   * Http timeout in milliseconds
   */
  @Parameter(names = "--http-timeout")
  @Min(1)
  public int httpTimeout = 60*1000;

  /**
   * Returns the directory with the decompressed archive folder created by the dwca downloader.
   */
  public File archiveDir(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString());
  }

  /**
   * Returns the dwc archive file within the data repository.
   */
  public File archiveFile(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString() + ".zip");
  }
}
