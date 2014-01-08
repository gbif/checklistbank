package org.gbif.checklistbank.ws.client;


import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.vocabulary.ThreatStatus;

import com.sun.jersey.api.client.WebResource;

public class DistributionWsClientTest extends WsClientNameUsageComponentTest<Distribution> {


  public DistributionWsClientTest() {
    super(Distribution.class);
  }


  @Override
  NameUsageComponentBaseWsClient<Distribution> getClient(WebResource resource) {
    return new DistributionWsClient(resource);
  }

  @Override
  Distribution getMockObject() {
    Distribution distribution = new Distribution();
    distribution.setThreatStatus(ThreatStatus.ENDANGERED);
    return distribution;
  }

}
