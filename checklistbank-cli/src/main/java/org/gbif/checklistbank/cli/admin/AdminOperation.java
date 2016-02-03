package org.gbif.checklistbank.cli.admin;

/**
 *
 */
public enum AdminOperation {
  CLEANUP,
  CRAWL,
  NORMALIZE,
  IMPORT,
  ANALYZE,
  REPARSE,
  CLEAN_ORPHANS,
  SYNC_DATASETS,
  UPDATE_VERBATIM,
  DUMP,
  VALIDATE_NEO,
  MATCH_DATASET
}
