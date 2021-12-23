package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.VernacularNameMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a VernacularNameService using MyBatis. */
@Service
public class VernacularNameServiceMyBatis extends NameUsageComponentServiceMyBatis<VernacularName>
    implements VernacularNameService {

  @Autowired
  VernacularNameServiceMyBatis(VernacularNameMapper vernacularNameMapper) {
    super(vernacularNameMapper);
  }
}
