package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.service.checklistbank.ReferenceService;

import java.util.List;

import com.google.inject.Inject;

/**
 * Implements a ReferenceService using MyBatis.
 */
public class ReferenceServiceMyBatis extends NameUsageComponentServiceMyBatis<Reference> implements ReferenceService {

  @Inject
  ReferenceServiceMyBatis(ReferenceMapper referenceMapper) {
    super(referenceMapper);
  }

  @Override
  public List<Reference> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }

}
