package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.MessagingConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@SuppressWarnings("PublicField")
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

}
