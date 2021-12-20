package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.mybatis.mapper.VernacularNameMapper;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a VernacularNameService using MyBatis.
 */
public class VernacularNameServiceMyBatis extends NameUsageComponentServiceMyBatis<VernacularName>
  implements VernacularNameService {

  @Autowired
  VernacularNameServiceMyBatis(VernacularNameMapper vernacularNameMapper) {
    super(vernacularNameMapper);
  }
}
