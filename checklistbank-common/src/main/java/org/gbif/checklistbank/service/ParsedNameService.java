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
   * Reparses all stored names in the db
   *
   * @return number of parsed names
   */
  int reparseAll();

  /**
   * Deletes all orphaned names without a name_usage linking to them
   * @return number of deleted names
   */
  int deleteOrphaned();
}
