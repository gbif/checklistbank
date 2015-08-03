package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.model.Constants;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

/**
 * Class that truncates tables and resets the sequences to the expected start values
 */
public class ClbInit {
    private static final List<String> TABLES = ImmutableList.of("name_usage", "name", "name_usage_metrics", "raw_usage", "citation");

    public static void reset(Connection con) {
        try {
            for (String table : TABLES) {
                try (Statement st = con.createStatement()) {
                    st.execute("TRUNCATE " + table + " CASCADE");
                }

                try (Statement st = con.createStatement()) {
                    st.execute("ALTER SEQUENCE " + table + "_id_seq RESTART 1");
                } catch (SQLException e) {
                    // ignore
                }
                // finally set name_usage to start at non nub values:
                try (Statement st = con.createStatement()) {
                    st.execute("ALTER SEQUENCE name_usage_id_seq RESTART " + (Constants.NUB_MAXIMUM_KEY+1));
                }
            }
        } catch (SQLException e) {
            Throwables.propagate(e);
        }
    }
}
