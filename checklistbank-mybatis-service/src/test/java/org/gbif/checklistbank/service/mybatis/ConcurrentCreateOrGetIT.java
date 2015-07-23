package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Before;
import org.junit.Test;

public class ConcurrentCreateOrGetIT {
    private static final String PROPERTY_FILE = "checklistbank.properties";
    private final int threads = 10;
    private Properties props;

    @Before
    public void init() throws Exception {
        props = PropertiesUtil.loadProperties(PROPERTY_FILE);
    }

    static class ClbMybatisCallable  {
        private final Properties props;
        private Injector inj;
        private HikariDataSource hds;

        public ClbMybatisCallable(Properties props) {
            this.props = props;
        }

        private void init() {
            // init mybatis layer and solr from cfgN instance
            inj = Guice.createInjector(new ChecklistBankServiceMyBatisModule(props));
            Key<DataSource> dsKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
            hds = (HikariDataSource) inj.getInstance(dsKey);

        }
        public Connection getConnection() throws SQLException {
            if (hds == null) {
                init();
            }
            return hds.getConnection();
        }

        public Injector getInj() {
            if (inj == null) {
                init();
            }
            return inj;
        }

        public void shutdown() {
            if (hds != null) {
                hds.close();
            }
        }

    }

    static class ParsedNameCallable extends ClbMybatisCallable implements Callable<ParsedName> {
        private final String name;
        public ParsedNameCallable(Properties props, String name) {
            super(props);
            this.name = name;
        }

        @Override
        public ParsedName call() throws Exception {
            ParsedNameService pservice = getInj().getInstance(ParsedNameService.class);
            CitationService cservice = getInj().getInstance(CitationService.class);
            for (int x=0; x<100; x++) {
                pservice.createOrGet(name + "citation #" +x);
                cservice.createOrGet(name + "citation #" +x);
            }
            ParsedName pn = pservice.createOrGet(name);
            shutdown();
            return pn;
        }
    }

    @Test
    public void writeNamesInParallel() throws Exception {
        final int tasks = 100;

        PrintStream log = System.out;

        // truncate tables
        log.println("Truncate existing data");
        ClbMybatisCallable mabat = new ClbMybatisCallable(props);
        Connection cn = mabat.getConnection();
        java.sql.Statement st = cn.createStatement();
        st.execute("truncate name_usage cascade");
        st.execute("truncate name cascade");
        st.execute("truncate citation cascade");
        st.close();
        cn.close();

        ExecutorCompletionService<ParsedName> ecs = new ExecutorCompletionService(Executors.<ParsedName>newFixedThreadPool(threads));
        LinkedList<Future<ParsedName>> futures = Lists.newLinkedList();

        for (int i = 0; i < tasks; i++) {
            ParsedNameCallable pnc = new ParsedNameCallable(props, "Umberto");
            log.println("Submitting task");
            futures.add(ecs.submit(pnc));
        }

        while(!futures.isEmpty()) {
            Future<ParsedName> f = futures.pop();
            ParsedName pn = f.get();
            if (pn != null) {
                log.println(pn.getKey() + " - " + pn.getScientificName());
            } else {
                log.println(pn);
            }
        }
        log.println("Finished all tasks. Done");
    }
}