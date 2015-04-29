package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.MatchServiceConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.checklistbank.nub.NubSource;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Lists;
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
  @Valid
  @NotNull
  public MatchServiceConfiguration matching = new MatchServiceConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  public List<UUID> sources = Lists.newArrayList();

  public List<NubSource> getSources() {
    List<NubSource> list = Lists.newArrayList();
    int priority = 1;
    for (UUID datasetKey : sources) {
      NubSource ns = new NubSource();
      ns.key = datasetKey;
      ns.name = datasetKey.toString();
      ns.priority = priority++;
      list.add(ns);
    }
    return list;
  }
}
