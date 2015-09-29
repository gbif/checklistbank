package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.net.URI;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@SuppressWarnings("PublicField")
public class NubConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(NubConfiguration.class);

    @ParametersDelegate
    @NotNull
    @Valid
    public MessagingConfiguration messaging = new MessagingConfiguration();

    @ParametersDelegate
    @Valid
    @NotNull
    public NeoConfiguration neo = new NeoConfiguration();

    @ParametersDelegate
    @Valid
    @NotNull
    public ClbConfiguration clb = new ClbConfiguration();

    @ParametersDelegate
    @NotNull
    @Valid
    public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

    @NotNull
    @Valid
    public URI sourceList = URI.create("https://raw.githubusercontent.com/gbif/checklistbank/master/checklistbank-nub/nub-sources.tsv");

    /**
     * If true an import message is being send once the build is done and basic backbone assertions have passed successfully.
     */
    @Valid
    public boolean autoImport = false;
}
