package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;

/**
 * Efficient serializing/deserializing mapper specifically for the term maps of a VerbatimNameUsage
 * to be stored in postgres or neo backends as a single binary column.
 */
public interface VerbatimNameUsageMapper {

  VerbatimNameUsage read(byte[] data);

  byte[] write(VerbatimNameUsage verbatim);

}
