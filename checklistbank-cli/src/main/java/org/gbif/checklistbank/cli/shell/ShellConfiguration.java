package org.gbif.checklistbank.cli.shell;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;

import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
public class ShellConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public NeoConfiguration neo;

  @Parameter(names = {"-k", "--key"}, required = false)
  @NotNull
  public UUID key = Constants.NUB_DATASET_KEY;

  @Parameter(names = {"--port"}, required = false)
  public int port = 1337;

}
