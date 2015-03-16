package org.gbif.nub.build;

import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.checklistbank.model.Usage;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.utils.file.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * Test mock implementation of the batch service loading json files from the dataset test directory
 * to answer the list and listNames methods. Paging is entirely ignored and always the full list of
 * records is returned!
 */
public class JsonMockService implements UsageService, ParsedNameService {
  private InputStreamUtils isu = new InputStreamUtils();
  private final ObjectMapper mapper;

  public JsonMockService() {
    mapper = new ObjectMapper();
    mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public List<Integer> listAll() {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Integer maxUsageKey(UUID datasetKey) {
    return 100000;
  }

  @Override
  public List<NameUsageContainer> listRange(int usageKeyStart, int usageKeyEnd) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Integer> listParents(int usageKey) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Usage> list(UUID datasetKey, Pageable page) {
    System.out.println("Read usages for " + datasetKey);
    InputStream json = usagesJson(datasetKey);
    TypeReference ref = new TypeReference<List<Usage>>() { };
    return list(json, ref, page);
  }

  @Override
  public ParsedName get(int key) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public ParsedName createOrGet(String scientificName) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<ParsedName> listNames(UUID datasetKey, Pageable page) {
    System.out.println("Read names for " + datasetKey);
    InputStream json = namesJson(datasetKey);
    TypeReference ref = new TypeReference<List<ParsedName>>() { };
    return list(json, ref, page);
  }

  private <T> PagingResponse<T> list(InputStream json, TypeReference<List<T>> ref, Pageable page) {
    try {
      List<T> data = mapper.readValue(json, ref);
      json.close();
      PagingResponse<T> resp = new PagingResponse<T>(page, null, data);
      resp.setEndOfRecords(true);
      return resp;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private InputStream namesJson(UUID datasetKey) {
    String file = prefix(datasetKey) + "-names.json";
    return isu.classpathStream(file);
  }

  private InputStream usagesJson(UUID datasetKey) {
    String file = prefix(datasetKey) + "-usages.json";
    return isu.classpathStream(file);
  }

  private String prefix(UUID datasetKey) {
    return "datasets/"+datasetKey.toString();
  }

}
