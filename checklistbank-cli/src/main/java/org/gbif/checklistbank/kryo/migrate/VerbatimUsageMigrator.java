package org.gbif.checklistbank.kryo.migrate;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.kryo.ClbKryoFactory;
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapperJson;
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapperKryo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Reads all raw usage data from the raw table and updates the kryo serialization data to the latest version.
 * It uses different kryo factories with slightly different class registrations in order to handle different serialization formats
 * created during indexing with different factories.
 */
public class VerbatimUsageMigrator {
    private static final int FETCH_SIZE = 1000;
    private final ClbConfiguration cfg;

    private VerbatimNameUsageMapper jsonMapper = new VerbatimNameUsageMapperJson();

    private List<VerbatimNameUsageMapperKryo> allMapper = Lists.newArrayList(
            new VerbatimNameUsageMapperKryo(new KryoFactory2_13()),
            new VerbatimNameUsageMapperKryo(new KryoFactory2_15()),
            new VerbatimNameUsageMapperKryo(new KryoFactory2_25()),
            new VerbatimNameUsageMapperKryo(new VerbatimNameUsageMapperKryo.VerbatimKryoFactory()),
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

            PreparedStatement update = c2.prepareStatement("UPDATE raw_usage SET json=? WHERE usage_fk=?");

            Statement st = c1.createStatement();
            st.setFetchSize(FETCH_SIZE);
            ResultSet rs = st.executeQuery("SELECT usage_fk, data FROM raw_usage WHERE json IS NULL");
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
                    update.setString(1, transform(rs.getInt(1), rs.getBytes(2)));
                    update.setInt(2, rs.getInt(1));
                    update.execute();
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

    private String transform(int usageKey, byte[] data) throws IllegalArgumentException {
        int midx = -1;
        for (VerbatimNameUsageMapper mapper : allMapper) {
            try {
                midx++;
                VerbatimNameUsage v = mapper.read(data);
                if (verifyInstance(v)) {
                    counters.get(midx).incrementAndGet();
                    String json = new String(jsonMapper.write(v), Charsets.UTF_8);
                    //System.out.println(midx+" "+usageKey+" "+json);
                    return json;
                }
            } catch (Exception e) {
                // ignore, try next
            }
        }
        throw new IllegalStateException("No liveMapper found to deserialize " + usageKey);
    }

    private boolean verifyInstance(VerbatimNameUsage v) {
        return v.getExtensions() instanceof Map
                && v.getFields() instanceof Map;
    }

    public static void main(String[] args) throws Exception {
        ClbConfiguration cfg = new ClbConfiguration();
        cfg.serverName = "pg1.gbif.org";
        cfg.databaseName = "checklistbank2";
        cfg.user = "clb";
        cfg.password = "%BBJu2MgstXJ";
        VerbatimUsageMigrator migrator = new VerbatimUsageMigrator(cfg);
        migrator.updateAll();
    }
}
