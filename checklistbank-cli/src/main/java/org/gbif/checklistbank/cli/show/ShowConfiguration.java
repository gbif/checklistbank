package org.gbif.checklistbank.cli.show;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.GraphFormat;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.UUID;

public class ShowConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public NeoConfiguration neo = new NeoConfiguration();

  @Parameter(names = {"-k", "--key"}, required = false)
  @Nullable
  public UUID key = Constants.NUB_DATASET_KEY;

  @Valid
  @Parameter(names = {"--id", "--root-id"}, required = false)
  public Long rootId;

  @Valid
  @Parameter(names = {"-n", "--name", "--root-name"}, required = false)
  public String rootName;

  @Valid
  @Parameter(names = {"-f", "--file"}, required = false)
  public File file;

  @Valid
  @Parameter(names = {"--format"}, required = false)
  public GraphFormat format = GraphFormat.TEXT;

  @Valid
  @Parameter(names = {"-fn", "--full-names"}, required = false)
  public boolean fullNames = false;

  @Valid
  @Parameter(names = {"-r", "--lowest-rank"}, required = false)
  public Rank lowestRank;

  @Valid
  @Parameter(names = {"--dao-report"}, required = false)
  public boolean daoReport = false;

}
