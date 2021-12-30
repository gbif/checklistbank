package org.gbif.checklistbank.index;

import org.gbif.checklistbank.index.backfill.SolrBackfill;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension to load data into Solr.
 */
public class SolrLoadRule implements BeforeEachCallback {

  private SolrBackfill backFill;
  private DataSource dataSource;

  public SolrLoadRule(SolrBackfill backFill, DataSource dataSource) {
    this.backFill = backFill;
    this.dataSource = dataSource;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    ClbDbTestRule.squirrels(dataSource).before();
    backFill.run();
  }
}
