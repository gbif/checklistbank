package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.cli.common.ClbConfiguration;

import java.sql.Connection;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UsageIteratorNeo implementation reading from a postgres checklistbank db
 * using the native postgres jdbc copy manager.
 *
 * At creation time the instance connects to an CLB instance and copies all the minimal information needed to build a
 * taxonomic tree into an embedded neo db. No extension data is copied, just core taxonomic information.
 */
public class ClbUsageIteratorNeo extends UsageIteratorNeo {
  private static final Logger LOG = LoggerFactory.getLogger(ClbUsageIteratorNeo.class);
  private final ClbConfiguration clb;

  public ClbUsageIteratorNeo(ClbConfiguration clb, NubSource source) throws Exception {
    super(source);
    this.clb = clb;
  }

  @Override
  void initNeo(NeoUsageWriter writer) throws Exception {
    try (Connection c = clb.connect()){
      final CopyManager cm = new CopyManager((BaseConnection) c);
      cm.copyOut("COPY ("
                 + "SELECT u.id, u.parent_fk, u.basionym_fk, u.rank, u.is_synonym, u.status, u.nom_status, n.scientific_name"
                 + " FROM name_usage u join name n ON name_fk=n.id" + " WHERE dataset_key = '" + source.key + "')"
                 + " TO STDOUT WITH NULL ''", writer);
      LOG.info("Loaded nub source data {} with {} usages into neo4j at {}", source.name, writer.getCounter(),
        neoDir.getAbsolutePath());
    }
  }

}
