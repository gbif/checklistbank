package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.MapDbObjectSerializer;
import org.gbif.checklistbank.postgres.TabMapperBase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdLookup {
    private static final Logger LOG = LoggerFactory.getLogger(IdLookup.class);
    private final DB db;
    private final Map<String, LookupUsage> usages;

    public IdLookup(ClbConfiguration clb) throws SQLException, IOException {
        db = DBMaker.tempFileDB()
                .fileMmapEnableIfSupported()
                .transactionDisable()
                .make();
        usages = db.hashMapCreate("usages")
                .keySerializer(Serializer.STRING_ASCII)
                .valueSerializer(new MapDbObjectSerializer<LookupUsage>(LookupUsage.class))
                .make();
        UsageWriter writer = new UsageWriter();
        try (Connection c = clb.connect()){
            final CopyManager cm = new CopyManager((BaseConnection) c);
            cm.copyOut("COPY ("
                    + "SELECT u.id, u.parent_fk, n.scientific_name, n.scientific_name, u.rank, u.status, u.kingdom_fk, false"
                    + " FROM name_usage u join name n ON name_fk=n.id"
                    + " WHERE dataset_key = '" + Constants.NUB_DATASET_KEY + "')"
                    + " TO STDOUT WITH NULL ''", writer);
            LOG.info("Loaded nub source data with {} usages into neo4j lookup", usages.size());
        } finally {
            writer.close();
        }
    }

    /**
     * int key
     * Integer parentKey
     * String canonical
     * String authorship
     * Rank rank
     * TaxonomicStatus status
     * Kingdom kingdom
     * boolean deleted
     */
    private class UsageWriter extends TabMapperBase {
        public UsageWriter() {
            // the number of columns in our query to consume
            super(8);
        }

        @Override
        protected void addRow(String[] row) {
            LookupUsage u = new LookupUsage(
                    toInt(row[0]),
                    toInt(row[1]),
                    row[2],
                    row[3],
                    Rank.valueOf(row[4]),
                    TaxonomicStatus.valueOf(row[5]),
                    Kingdom.byNubUsageId(Integer.valueOf(row[6])),
                    "t".equals(row[7])
            );
            usages.put(u.getCanonical(), u);
        }

        private Integer toInt(String x) {
            return x == null ? null : Integer.valueOf(x);
        }
    }

    public LookupUsage match(String canonicalName, String authorship, Rank rank, Kingdom kingdom, TaxonomicStatus status) {
        return usages.get(canonicalName);
    }

    /**
     * @return the number of known usage keys
     */
    public int size() {
        return usages.size();
    }
}
