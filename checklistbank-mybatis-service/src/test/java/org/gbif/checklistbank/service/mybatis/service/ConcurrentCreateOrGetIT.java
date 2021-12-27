package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.nameparser.NameParserGbifV1;

import java.io.PrintStream;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ConcurrentCreateOrGetIT extends MyBatisServiceITBase {
  private final ParsedNameService parsedNameService;
  private final CitationService citationService;

  @Autowired
  public ConcurrentCreateOrGetIT(
      DataSource dataSource, ParsedNameService parsedNameService, CitationService citationService) {
    super(dataSource);
    this.parsedNameService = parsedNameService;
    this.citationService = citationService;
  }

  static class ParsedNameCallable implements Callable<ParsedName> {
    private final String name;
    private static final NameParser PARSER = new NameParserGbifV1();
    private final ParsedNameService parsedNameService;
    private final CitationService citationService;

    public ParsedNameCallable(
        String name, ParsedNameService parsedNameService, CitationService citationService) {
      this.name = name;
      this.parsedNameService = parsedNameService;
      this.citationService = citationService;
    }

    @Override
    public ParsedName call() throws Exception {

      for (int x = 0; x < 100; x++) {
        parsedNameService.createOrGet(PARSER.parse(name + " " + x + "-banales", null), true);
        citationService.createOrGet(name + " citation #" + x);
      }
      ParsedName pn = parsedNameService.createOrGet(PARSER.parse(name, null), true);
      return pn;
    }
  }

  @Test
  public void writeNamesInParallel() throws Exception {
    final int tasks = 100;

    PrintStream log = System.out;

    // truncate tables
    log.println("Truncate existing data");
    Connection cn = dataSource.getConnection();
    java.sql.Statement st = cn.createStatement();
    st.execute("truncate name_usage cascade");
    st.execute("truncate name cascade");
    st.execute("truncate citation cascade");
    st.close();
    cn.close();

    int threads = 10;
    ExecutorCompletionService<ParsedName> ecs =
        new ExecutorCompletionService<>(Executors.newFixedThreadPool(threads));
    LinkedList<Future<ParsedName>> futures = Lists.newLinkedList();

    for (int i = 0; i < tasks; i++) {
      ParsedNameCallable pnc =
          new ParsedNameCallable("Umberto", parsedNameService, citationService);
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
