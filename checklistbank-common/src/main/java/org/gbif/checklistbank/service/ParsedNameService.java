package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.ParsedName;

/**
 * Persistence service dealing with parsed names.
 * This interface is restricted to the mybatis module only!
 */
public interface ParsedNameService {

  /**
   * Retrieves the matching stored parsed name with id or
   * inserts non existing names into the name_string table using the pre-parsed name.
   */
  ParsedName createOrGet(ParsedName name);

  /**
   * Deletes all orphaned names without a name_usage linking to them
   * @return number of deleted names
   */
  int deleteOrphaned();

  /**
   * Reparses all scientific names from the names table and updates modified parsed name records
   * @return number of changed parsed names
   */
  int reparseAll();
}
