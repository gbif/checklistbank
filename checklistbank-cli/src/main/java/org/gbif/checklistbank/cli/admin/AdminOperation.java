package org.gbif.checklistbank.cli.admin;

/**
 *
 */
public enum AdminOperation {
  /**
   * Removes entries in zookeeper and clears the local dwca repository files for the requested dataset keys.
   * This works for any dataset, not only checklists.
   */
  CLEANUP(false),

  /**
   * Sends a StartCrawlMessage message for the requested datasets.
   * This works for any dataset, not only checklists.
   */
  CRAWL(false),

  /**
   * Sends a DwcaMetasyncFinishedMessage message for the requested datasets to trigger a dataset normalization in neo4j.
   */
  NORMALIZE(false),

  /**
   * Sends a ChecklistNormalizedMessage message for the requested datasets to trigger an import of a checklist from neo4j into postgres checklistbank.
   */
  IMPORT(false),

  /**
   * Sends a ChecklistSyncedMessage message for the requested datasets to trigger a dataset analysis within postgres.
   */
  ANALYZE(false),

  /**
   * Sends a MatchDatasetMessage message for the requested datasets to trigger a checklist name matching against the backbone.
   */
  MATCH(false),

  /**
   * Sends a MatchDatasetMessage message for all checklist datasets excluding the nub and the CoL.
   * This is useful to rematch all checklists after a new backbone has been created.
   * Note: The CoL should first be matched manually on its own before all other checklists are matched.
   * This is needed to establish the right dataset metrics for the CoL overlap!
   */
  REMATCH(true),

  /**
   * Exports a requested checklist dataset into a dwc archive.
   */
  EXPORT(false),

  /**
   * Dumps a checklist from postgres to the local neo4j repository.
   */
  DUMP(false),


  /**
   * Sends a BackboneChanged message.
   */
  NUB_CHANGED(true),

  /**
   * Checks
   */
  NUB_CHECK(true),

  /**
   * Updates the backbone dataset metadata based on the current dataset metrics
   */
  UPDATE_NUB_DATASET(true),

  /**
   * Reparse all parsed names.
   */
  REPARSE(true),

  /**
   * Cleans up orphan name records in checklistbank.
   */
  CLEAN_ORPHANS(true),

  /**
   * Syncs the checklistbank dataset table against the current registry checklist datasets.
   */
  SYNC_DATASETS(true),

  /**
   * Runs the backbone validation against the local neo4j backbone.
   */
  VALIDATE_NEO(true),
  
  /**
   * Runs the backbone validation against a postgres database.
   */
  VALIDATE_BACKBONE(true),

  /**
   * Runs a liquibase schema update for the configured database.
   */
  SCHEMA_UPDATE(true);

  public final boolean global;

  AdminOperation(boolean global) {
    this.global = global;
  }
}
