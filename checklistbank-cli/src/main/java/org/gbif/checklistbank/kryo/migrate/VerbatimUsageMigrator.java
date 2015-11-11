package org.gbif.checklistbank.kryo.migrate;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.kryo.ClbKryoFactory;
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapperKryo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Reads all raw usage data from the raw table and updates the kryo serialization data to the latest version.
 * It uses different kryo factories with slightly different class registrations in order to handle different serialization formats
 * created during indexing with different factories.
 */
public class VerbatimUsageMigrator {
    private static final int FETCH_SIZE = 100;
    private VerbatimNameUsageMapperKryo mapper = new VerbatimNameUsageMapperKryo(new ClbKryoFactory1Curr());
    private final ClbConfiguration cfg;

    private List<VerbatimNameUsageMapperKryo> allMapper = Lists.newArrayList(mapper,
            new VerbatimNameUsageMapperKryo(new ClbKryoFactory4()),
            new VerbatimNameUsageMapperKryo(new ClbKryoFactory2()),
            new VerbatimNameUsageMapperKryo(new ClbKryoFactory3()),
            new VerbatimNameUsageMapperKryo(new ClbKryoFactory())
    );
    private List<AtomicInteger> counters = Lists.newArrayList();

    public VerbatimUsageMigrator(ClbConfiguration cfg) {
        this.cfg = cfg;
    }

    public void updateAll() throws Exception {
        for (VerbatimNameUsageMapperKryo m : allMapper){
            counters.add(new AtomicInteger(0));
        }
        try (Connection c1 = cfg.connect(); Connection c2 = cfg.connect()) {
            // make sure autocommit is off
            c1.setAutoCommit(false);
            c2.setAutoCommit(false);

            PreparedStatement update = c2.prepareStatement("UPDATE raw_usage SET migrated=true, data=? WHERE usage_fk=?");

            Statement st = c1.createStatement();
            st.setFetchSize(FETCH_SIZE);
            ResultSet rs = st.executeQuery("SELECT usage_fk, data FROM raw_usage WHERE NOT migrated");
            int counter = 0;
            int error = 0;
            Joiner countJoiner = Joiner.on("-");
            while (rs.next()) {
                if (counter % FETCH_SIZE == 0) {
                    System.out.println(counter + " updated, " + error + ": " + countJoiner.join(counters));
                    c2.commit();
                }
                counter++;
                // transform
                try {
                    update.setBytes(1, transform(rs.getInt(1), rs.getBytes(2)));
                    update.setInt(2, rs.getInt(1));
                    //update.execute();
                } catch (Exception e) {
                    error++;
                    System.err.println("Failed to transform usage " + rs.getInt(1));
                    e.printStackTrace();
                }
            }
            System.out.println(counter + " updated, " + error + " errors: " + countJoiner.join(counters));
            rs.close();
            st.close();
            c2.commit();
            update.close();
            System.out.println("Updated all.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private byte[] transform(int usageKey, byte[] data) throws IllegalArgumentException {
        int midx = -1;
        for (VerbatimNameUsageMapper mapper : allMapper) {
            try {
                midx++;
                VerbatimNameUsage v = mapper.read(data);
                if (verifyInstance(v)) {
                    counters.get(midx).incrementAndGet();
                    return mapper.write(v);
                }
                System.out.print(midx+" "+usageKey+" ");
                System.out.println(v);
            } catch (Exception e) {
                // ignore, try next
            }
        }
        throw new IllegalStateException("No mapper found to deserialize " + usageKey);
    }

    private boolean verifyInstance(VerbatimNameUsage v) {
        return v.getExtensions() instanceof Map
                && v.getFields() instanceof Map;
    }

    public static void main(String[] args) throws Exception {
        ClbConfiguration cfg = new ClbConfiguration();
        cfg.serverName = "pg1.gbif-uat.org";
        cfg.databaseName = "uat_checklistbank";
        cfg.user = "clb";
        cfg.password = "%BBJu2MgstXJ";
        VerbatimUsageMigrator migrator = new VerbatimUsageMigrator(cfg);
        migrator.updateAll();
    }
}
