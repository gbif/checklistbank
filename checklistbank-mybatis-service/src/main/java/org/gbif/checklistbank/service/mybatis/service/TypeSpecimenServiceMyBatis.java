package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.TypeSpecimenMapper;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a TypeSpecimenService using MyBatis. */
@Service
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
