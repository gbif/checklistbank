package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.config.ClbConfiguration;

import java.util.UUID;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A nub source which is backed by postgres checklistbank usages of a given datasetKey
 */
public class ClbSource extends NubSource {
    private static final Logger LOG = LoggerFactory.getLogger(ClbSource.class);

    private final ClbConfiguration clb;

    public ClbSource(ClbConfiguration clb, UUID key, String name) {
      super(key, name.replaceAll("\\s", " "), false);
      this.clb = clb;
    }

    public ClbSource(ClbConfiguration clb, Dataset dataset) {
        this(clb, dataset.getKey(), dataset.getTitle());
    }

    @Override
    void initNeo(NeoUsageWriter writer) throws Exception {
        try (BaseConnection c = (BaseConnection) clb.connect()) {
            final CopyManager cm = new CopyManager(c);
            cm.copyOut("COPY ("
                    + "SELECT u.id, u.parent_fk, u.basionym_fk, u.rank,"
                    + " coalesce(u.status, CASE WHEN (u.is_synonym) THEN 'SYNONYM'::taxonomic_status ELSE 'ACCEPTED'::taxonomic_status END),"
                    + " u.nom_status, n.scientific_name"
                    + " FROM name_usage u JOIN name n ON u.name_fk=n.id"
                    + " WHERE u.dataset_key = '" + key + "')"
                    + " TO STDOUT WITH NULL ''", writer);
        }
    }
}
