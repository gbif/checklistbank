package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.cli.common.ClbConfiguration;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UsageIteratorNeo implementation reading from a postgres checklistbank db
 * using the native postgres jdbc copy manager.
 *
 * At creation time the instance connects to an CLB instance and copies all the minimal information needed to build a
 * taxonomic tree into an embedded, persistent neo db. No extension data is copied, just core taxonomic information.
 */
public class ClbUsageIteratorNeo extends UsageIteratorNeo {
  private static final Logger LOG = LoggerFactory.getLogger(ClbUsageIteratorNeo.class);
  private final ClbConfiguration clb;

  public ClbUsageIteratorNeo(ClbConfiguration clb, NubSource source) throws Exception {
    super(source, 0);
    this.clb = clb;
  }

  @Override
  void initNeo(NeoUsageWriter writer) throws Exception {
    try (BaseConnection c = clb.connect()){
      final CopyManager cm = new CopyManager(c);
      cm.copyOut("COPY ("
                 + "SELECT usage.id, usage.parent_fk, usage.basionym_fk, usage.rank, usage.is_synonym, usage.status, usage.nom_status, node.scientific_name"
                 + " FROM name_usage usage join name node ON name_fk=node.id" + " WHERE dataset_key = '" + source.key + "')"
                 + " TO STDOUT WITH NULL ''", writer);
      LOG.info("Loaded nub source data {} with {} usages into neo4j", source.name, writer.getCounter());
    }
  }

}
