package org.gbif.checklistbank.cli.show;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;

import java.io.File;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
public class ShowConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public NeoConfiguration neo;

  @Parameter(names = {"-k", "--key"}, required = false)
  @Nullable
  public UUID key = Constants.NUB_DATASET_KEY;

  @Valid
  @Parameter(names = {"--usage-key"}, required = false)
  public Long usageKey;

  @Valid
  @Parameter(names = {"-f", "--file"}, required = false)
  public File file;

  @Valid
  @Parameter(names = {"--format"}, required = false)
  public GraphFormat format = GraphFormat.TEXT;

  @Valid
  @Parameter(names = {"--full-names"}, required = false)
  public boolean fullNames = false;
}
