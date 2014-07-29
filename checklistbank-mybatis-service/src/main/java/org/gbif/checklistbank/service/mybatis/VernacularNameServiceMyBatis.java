package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.mybatis.mapper.VernacularNameMapper;

import com.google.inject.Inject;

/**
 * Implements a VernacularNameService using MyBatis.
 */
public class VernacularNameServiceMyBatis extends NameUsageComponentServiceMyBatis<VernacularName>
  implements VernacularNameService {

  @Inject
  VernacularNameServiceMyBatis(VernacularNameMapper vernacularNameMapper) {
    super(vernacularNameMapper);
  }
}
