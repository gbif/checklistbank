package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;

/**
 *
 */
public interface MatchingService extends NameUsageMatchingService {

  /**
   * Strict match of a pre-parsed name
   * @return nub usage key or null of no match
   */
  Integer matchStrict(ParsedName pn, String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification);

}
