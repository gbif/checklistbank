package org.gbif.checklistbank.service.mybatis;


import org.gbif.checklistbank.service.CitationService;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

public class CitationServiceMyBatis implements CitationService {
  private CitationMapper mapper;

  @Inject
  CitationServiceMyBatis(CitationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public String get(int key) {
    return mapper.get(key);
  }

  @Override
  public Integer createOrGet(String citation) {
    checkArgument(!Strings.isNullOrEmpty(citation), "A citation string is required");
    Integer key = mapper.getByCitation(citation);
    if (key == null) {
      key = mapper.create(citation);
    }
    return key;
  }

}
