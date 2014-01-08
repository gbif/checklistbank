package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.ws.client.guice.NameUsageMatchWs;
import org.gbif.checklistbank.ws.util.SimpleParameterMap;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the ChecklistUsageService.
 */
public class NameUsageMatchWsClient implements NameUsageMatchingService {

  private final WebResource resource;

  @Inject
  public NameUsageMatchWsClient(@NameUsageMatchWs WebResource resource) {
    this.resource = resource;
  }

  @Override
  public NameUsageMatch match(String scientificName, @Nullable Rank rank,
    @Nullable LinneanClassification classification, boolean strict, boolean verbose) {

    SimpleParameterMap parameters = new SimpleParameterMap()
      .param("name", scientificName)
      .param("strict", Boolean.toString(strict))
      .param("verbose", Boolean.toString(verbose));

    if (classification != null) {
      parameters.param("kingdom", classification.getKingdom())
        .param("phylum", classification.getPhylum())
        .param("class", classification.getClazz())
        .param("order", classification.getOrder())
        .param("family", classification.getFamily())
        .param("genus", classification.getGenus())
        .param("subgenus", classification.getSubgenus());
    }
    if (rank != null) {
      parameters.param("rank", rank.name());
    }

    return resource.queryParams(parameters).type(MediaType.APPLICATION_JSON).get(NameUsageMatch.class);
  }

  @Override
  public String toString() {
    return "NameUsageMatchWsClient{" + resource + '}';
  }
}
