package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.cli.common.ClbConfiguration;

import java.util.UUID;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UsageIteratorNeo implementation reading from a postgres checklistbank db
 * using the native postgres jdbc copy manager.
 * At creation time the instance connects to an CLB instance and copies all the minimal information needed to build a
 * taxonomic tree into an embedded, persistent neo db. No extension data is copied, just core taxonomic information.
 */
public class ClbUsageIteratorNeo extends UsageIteratorNeo {
    private static final Logger LOG = LoggerFactory.getLogger(ClbUsageIteratorNeo.class);
    private final ClbConfiguration clb;

    public ClbUsageIteratorNeo(ClbConfiguration clb, NubSource source) {
        this(clb, source.key, source.name);
    }

    public ClbUsageIteratorNeo(ClbConfiguration clb, UUID datasetKey, String datasetTitle) {
        super(0, datasetKey, datasetTitle);
        this.clb = clb;
    }

    @Override
    void initNeo(NeoUsageWriter writer) throws Exception {
        try (BaseConnection c = clb.connect()) {
            final CopyManager cm = new CopyManager(c);
            cm.copyOut("COPY ("
                    + "SELECT u.id, u.parent_fk, u.basionym_fk, u.rank,"
                    + " coalesce(u.status, CASE WHEN (u.is_synonym) THEN 'SYNONYM'::taxonomic_status ELSE 'ACCEPTED'::taxonomic_status END),"
                    + " u.nom_status, n.scientific_name"
                    + " FROM name_usage u JOIN name n ON u.name_fk=n.id"
                    + " WHERE u.dataset_key = '" + datasetKey + "')"
                    + " TO STDOUT WITH NULL ''", writer);
            LOG.info("Loaded nub source data {} with {} usages into neo4j", datasetTitle, writer.getCounter());
        }
    }

}
