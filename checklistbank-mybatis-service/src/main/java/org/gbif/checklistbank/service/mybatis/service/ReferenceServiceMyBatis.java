package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.ReferenceMapper;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a ReferenceService using MyBatis. */
@Service
public class ReferenceServiceMyBatis extends NameUsageComponentServiceMyBatis<Reference>
    implements ReferenceService {

  @Autowired
  ReferenceServiceMyBatis(ReferenceMapper referenceMapper) {
    super(referenceMapper);
  }

  @Override
  public Map<Integer, List<Reference>> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }
}
