package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.SpeciesProfileMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a SpeciesProfileService using MyBatis. */
@Service
public class SpeciesProfileServiceMyBatis extends NameUsageComponentServiceMyBatis<SpeciesProfile>
    implements SpeciesProfileService {

  @Autowired
  SpeciesProfileServiceMyBatis(SpeciesProfileMapper speciesProfileMapper) {
    super(speciesProfileMapper);
  }
}
