package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.service.checklistbank.TypeSpecimenService;

import java.util.List;

import com.google.inject.Inject;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Implements a TypeSpecimenService using MyBatis.
 */
public class TypeSpecimenServiceMyBatis extends NameUsageComponentServiceMyBatis<TypeSpecimen>
  implements TypeSpecimenService {

  @Inject
  TypeSpecimenServiceMyBatis(TypeSpecimenMapper typeSpecimenMapper, SqlSessionFactory sqlSessionFactory) {
    super(typeSpecimenMapper);
  }

  @Override
  public List<TypeSpecimen> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }
}
