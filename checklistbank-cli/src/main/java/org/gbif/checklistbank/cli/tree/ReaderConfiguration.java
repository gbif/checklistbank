package org.gbif.checklistbank.cli.tree;

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
@SuppressWarnings("PublicField")
public class ReaderConfiguration {

    @ParametersDelegate
    @Valid
    @NotNull
    public NeoConfiguration neo = new NeoConfiguration();

    @NotNull
    @Valid
    @Parameter(names = {"-d", "--dataset-key"}, required = false)
    public UUID datasetKey = Constants.NUB_DATASET_KEY;

    @Valid
    @Parameter(names = {"-k", "--key"}, required = false)
    public Long key = null;
}
