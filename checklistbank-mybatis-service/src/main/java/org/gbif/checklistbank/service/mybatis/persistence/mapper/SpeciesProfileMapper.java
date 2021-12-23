package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.SpeciesProfile;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for SpeciesProfile.
 */
public interface SpeciesProfileMapper extends NameUsageComponentMapper<SpeciesProfile> {

  void insert(@Param("key") int usageKey, @Param("obj") SpeciesProfile speciesProfile, @Param("sourceKey") Integer sourceKey);

}
