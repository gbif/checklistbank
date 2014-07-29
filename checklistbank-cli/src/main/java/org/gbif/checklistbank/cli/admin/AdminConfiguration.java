package org.gbif.checklistbank.cli.admin;

import org.gbif.checklistbank.cli.common.MessagingConfiguration;

import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
public class AdminConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(names = {"-d", "--dataset-key"}, required = true)
  @NotNull
  public UUID datasetKey;

  @Parameter(names = {"-op", "--operation"}, required = true)
  @NotNull
  public AdminOperation operation;


}
