package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.checklistbank.service.mybatis.mapper.TypeSpecimenMapper;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a TypeSpecimenService using MyBatis.
 */
public class TypeSpecimenServiceMyBatis extends NameUsageComponentServiceMyBatis<TypeSpecimen>
  implements TypeSpecimenService {

  @Autowired
  TypeSpecimenServiceMyBatis(TypeSpecimenMapper typeSpecimenMapper) {
    super(typeSpecimenMapper);
  }

  @Override
  public Map<Integer, List<TypeSpecimen>> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }
}
