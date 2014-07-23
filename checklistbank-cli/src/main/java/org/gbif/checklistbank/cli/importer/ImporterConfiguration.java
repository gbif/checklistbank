package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.MessagingConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;

import java.io.File;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
public class ImporterConfiguration {

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

  @ParametersDelegate
  @Valid
  @NotNull
  public ClbConfiguration clb = new ClbConfiguration();

  @Parameter(names = "--archive-repository")
  @NotNull
  public File archiveRepository;

  /**
   * Returns the directory with the decompressed archive folder created by the dwca downloader.
   */
  public File archiveDir(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString());
  }

}
