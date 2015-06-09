package org.gbif.checklistbank.service.mybatis;


import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.mybatis.mapper.CitationMapper;

import com.google.common.base.Strings;
import com.google.inject.Inject;

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
  public synchronized Integer createOrGet(String citation) {
    if (Strings.isNullOrEmpty(citation)) {
      return null;
    }
    Integer key = mapper.getByCitation(citation);
    if (key == null) {
      Citation c = new Citation(citation);
      mapper.insert(c);
      key = c.getKey();
    }
    return key;
  }

}
