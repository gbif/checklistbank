package org.gbif.checklistbank.cli.analysis;

import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.common.search.solr.SolrConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DatasetIndexUpdater implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexUpdater.class);

  private final ClbConfiguration clb;
  private SolrClient solr;


  public DatasetIndexUpdater(ClbConfiguration clb, SolrConfig dataset) {
    this.clb = clb;
    solr = dataset.buildSolr();
  }

  public void index(UUID datasetKey) {
    run(datasetKey);
  }

  public void indexAll() {
    run(null);
  }

  private void run(@Nullable UUID datasetKey) {
    try (Connection conn = clb.connect()) {

      conn.setAutoCommit(false);
      Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      st.setFetchSize(2);

      String sql = "SELECT dataset_key, array_agg(nub_fk) as keys " +
                    " FROM nub_rel";
      if (datasetKey != null) {
        sql = sql + " WHERE dataset_key='" + datasetKey.toString() + "'";
      }
      sql = sql +   " GROUP BY dataset_key";

      ResultSet rs = st.executeQuery(sql);
      while (rs.next()) {
        updDataset(rs);
      }
      rs.close();
      solr.commit();
      LOG.info("Done indexing taxon keys");

    } catch (Exception e) {
      LOG.error("Failed to index taxon keys for dataset index", e);

    } finally {
      try {
        solr.close();
      } catch (IOException e) {
        LOG.error("Failed to close solr", e);
      }
    }
  }

  private void updDataset(ResultSet rs) {
    try {
      String key = rs.getString("dataset_key");
      LOG.debug("Index taxon keys for dataset {}", key);
      Integer[] keys = (Integer[])rs.getArray("keys").getArray();
      LOG.debug("Indexing {} taxon keys for dataset {}", keys.length, key);

      SolrInputDocument doc = new SolrInputDocument();
      doc.addField("key", key);
      doc.addField("taxon_key", atomicUpdate(keys));
      doc.addField("record_count", keys.length);

      solr.add( doc );

    } catch (Exception e) {
      Throwables.propagate(e);
    }
  }

  private Map<String, Object> atomicUpdate(Object value) {
    Map<String, Object> atomic = Maps.newHashMap();
    atomic.put("set", value); // set, add, remove, inc
    return atomic;
  }

  @Override
  public void close() throws Exception {
    solr.close();
  }
}
