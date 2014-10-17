package org.gbif.checklistbank.cli.admin;

import org.gbif.checklistbank.cli.common.MessagingConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;

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

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  @Parameter(names = {"-d", "--dataset-key"}, required = false)
  public UUID datasetKey;

  @Parameter(names = {"-p", "--publisher-key"}, required = false)
  public UUID publisherKey;

  @Parameter(names = {"-op", "--operation"}, required = true)
  @NotNull
  public AdminOperation operation;

}
