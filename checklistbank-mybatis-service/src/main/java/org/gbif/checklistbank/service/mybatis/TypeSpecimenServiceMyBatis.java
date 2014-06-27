package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.service.checklistbank.TypeSpecimenService;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

/**
 * Implements a TypeSpecimenService using MyBatis.
 */
public class TypeSpecimenServiceMyBatis extends NameUsageComponentServiceMyBatis<TypeSpecimen>
  implements TypeSpecimenService {

  @Inject
  TypeSpecimenServiceMyBatis(TypeSpecimenMapper typeSpecimenMapper) {
    super(typeSpecimenMapper);
  }

  @Override
  public Map<Integer, List<TypeSpecimen>> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }
}
