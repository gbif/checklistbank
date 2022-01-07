package org.gbif.checklistbank.cli.shell;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

public class ShellConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public NeoConfiguration neo;

  @Parameter(names = {"-k", "--key"}, required = false)
  @NotNull
  public UUID key = Constants.NUB_DATASET_KEY;

}
