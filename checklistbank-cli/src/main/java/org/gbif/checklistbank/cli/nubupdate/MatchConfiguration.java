package org.gbif.checklistbank.cli.nubupdate;

import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.MessagingConfiguration;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;
import org.gbif.common.search.inject.SolrConfig;

import java.util.Properties;
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
  @Valid
  @NotNull
  public String matchWsUrl = "http://api.gbif.org/v1/species/match";

  public ChecklistBankWsClientModule createClientModule() {
    Properties props = new Properties();
    props.put("checklistbank.match.ws.url", matchWsUrl);
    return new ChecklistBankWsClientModule(props, false, false, true);
  }

}
