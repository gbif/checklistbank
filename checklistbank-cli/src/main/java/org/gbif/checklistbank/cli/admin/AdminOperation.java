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
  NUB_CHANGED,
  REPARSE,
  CLEAN_ORPHANS,
  SYNC_DATASETS,
  UPDATE_VERBATIM,
  DUMP,
  VALIDATE_NEO,
  MATCH_DATASET,
  UPDATE_NUB_NAMES,
  EXPORT
}
