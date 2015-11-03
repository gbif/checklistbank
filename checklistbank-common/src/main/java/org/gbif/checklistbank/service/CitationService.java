package org.gbif.checklistbank.service;

/**
 * Persistence service dealing with simple reference citations.
 * This interface is restricted to the mybatis module only!
 */
public interface CitationService {

  /**
   * Returns the key for an existing or newly inserted citation string.
   */
  Integer createOrGet(String citation);

    /**
     * Returns the key for an existing or newly inserted citation string.
     */
  Integer createOrGet(String citation, String doi, String link);
}
