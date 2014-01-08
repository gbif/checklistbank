package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.vocabulary.TypeStatus;

import com.sun.jersey.api.client.WebResource;

public class TypeSpecimenWsClientTest extends WsClientNameUsageComponentTest<TypeSpecimen> {

  public TypeSpecimenWsClientTest() {
    super(TypeSpecimen.class);
  }

  @Override
  TypeSpecimen getMockObject() {
    TypeSpecimen typeSpecimen = new TypeSpecimen();
    typeSpecimen.setTypeStatus(TypeStatus.HOLOTYPE);
    return typeSpecimen;
  }

  @Override
  NameUsageComponentBaseWsClient<TypeSpecimen> getClient(WebResource resource) {
    return new TypeSpecimenWsClient(resource);
  }

}
