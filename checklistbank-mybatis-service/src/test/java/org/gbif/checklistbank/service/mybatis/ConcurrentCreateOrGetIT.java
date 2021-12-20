package org.gbif.checklistbank.service.mybatis;

import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisConfiguration;
import org.gbif.nameparser.NameParserGbifV1;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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

public class ConcurrentCreateOrGetIT {
  private static final String PROPERTY_FILE = "checklistbank.properties";
  private final int threads = 10;
  private Properties props;

  @Before
  public void init() throws Exception {
    props = PropertiesUtil.loadProperties(PROPERTY_FILE);
  }

  static class ClbMybatisCallable {
    private final Properties props;
    private AnnotationConfigApplicationContext ctx;
    private HikariDataSource hds;

    public ClbMybatisCallable(Properties props) {
      this.props = props;
    }

    private void init() {
      // init mybatis layer and solr from cfgN instance
       ctx = new AnnotationConfigApplicationContext();
       ctx.register(ChecklistBankServiceMyBatisConfiguration.class);

      hds = (HikariDataSource) ctx.getBean(DataSource.class);
    }

    public Connection getConnection() throws SQLException {
      if (hds == null) {
        init();
      }
      return hds.getConnection();
    }

    public ConfigurableApplicationContext getAppContext() {
      if (ctx == null) {
        init();
      }
      return ctx;
    }

    public void shutdown() {
      if (hds != null) {
        hds.close();
      }
    }

  }

  static class ParsedNameCallable extends ClbMybatisCallable implements Callable<ParsedName> {
    private final String name;
    private static final NameParser PARSER = new NameParserGbifV1();

    public ParsedNameCallable(Properties props, String name) {
      super(props);
      this.name = name;
    }

    @Override
    public ParsedName call() throws Exception {
      ParsedNameService pservice = getAppContext().getBean(ParsedNameService.class);
      CitationService cservice = getAppContext().getBean(CitationService.class);
      for (int x = 0; x < 100; x++) {
        pservice.createOrGet(PARSER.parse(name + " " + x + "-banales", null), true);
        cservice.createOrGet(name + " citation #" + x);
      }
      ParsedName pn = pservice.createOrGet(PARSER.parse(name, null), true);
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

    ExecutorCompletionService<ParsedName> ecs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(threads));
    LinkedList<Future<ParsedName>> futures = Lists.newLinkedList();

    for (int i = 0; i < tasks; i++) {
      ParsedNameCallable pnc = new ParsedNameCallable(props, "Umberto");
      log.println("Submitting task");
      futures.add(ecs.submit(pnc));
    }

    while (!futures.isEmpty()) {
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