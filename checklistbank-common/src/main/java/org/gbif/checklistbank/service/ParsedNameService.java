package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;

import java.util.UUID;

/**
 * Persistence service dealing with parsed names.
 * This interface is restricted to the mybatis module only!
 */
public interface ParsedNameService {

  /**
   * Get a parsed name by its name key.
   * @param key the key of the parsed name
   * @return the parsed name or null
   */
  ParsedName get(int key);

  /**
   * Returns the full parsed name for a given scientific name string.
   * Inserts any non existing names into the name_string table.
   */
  ParsedName createOrGet(String scientificName);

  /**
   * Page through all names in a dataset.
   */
  PagingResponse<ParsedName> listNames(UUID datasetKey, Pageable page);

}
