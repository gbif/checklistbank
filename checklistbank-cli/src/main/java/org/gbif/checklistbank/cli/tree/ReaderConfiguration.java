package org.gbif.checklistbank.cli.tree;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;

import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

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
    public UUID datasetKey = Constants.NUB_DATASET_KEY;

    @Null
    @Valid
    public Long key = null;
}
