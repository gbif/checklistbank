package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.common.search.inject.SolrConfig;

import java.io.File;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@SuppressWarnings("PublicField")
public class NubChangedConfiguration {

    @ParametersDelegate
    @Valid
    @NotNull
    public GangliaConfiguration ganglia = new GangliaConfiguration();

    @ParametersDelegate
    @NotNull
    @Valid
    public MessagingConfiguration messaging = new MessagingConfiguration();

    @ParametersDelegate
    @Valid
    @NotNull
    public ClbConfiguration clb = new ClbConfiguration();

    @ParametersDelegate
    @Valid
    @NotNull
    public SolrConfig solr = new SolrConfig();

    @ParametersDelegate
    @NotNull
    @Valid
    public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

    @Parameter(names = "--export-repository")
    @NotNull
    public File exportRepository = new File("./exports");
}
