package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.common.search.inject.SolrConfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@SuppressWarnings("PublicField")
public class MatchConfiguration {

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

}
