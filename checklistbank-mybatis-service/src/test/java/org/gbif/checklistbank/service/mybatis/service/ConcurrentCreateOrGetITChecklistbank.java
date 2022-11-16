/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.service.mybatis.service;

import com.google.common.collect.Lists;
import org.gbif.ChecklistbankMyBatisServiceITBase;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.utils.NameParsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentCreateOrGetITChecklistbank extends ChecklistbankMyBatisServiceITBase {

  //Thread pool size
  private static final int NUM_THREADS = 10;

  //Total of parsing tasks to trigger
  private static final int NUM_TASKS = 100;

  private static PrintStream LOG = System.out;

  private final ParsedNameService parsedNameService;
  private final CitationService citationService;

  @Autowired
  public ConcurrentCreateOrGetITChecklistbank(
    DataSource dataSource, ParsedNameService parsedNameService, CitationService citationService) {
    super(dataSource);
    this.parsedNameService = parsedNameService;
    this.citationService = citationService;
  }

  static class ParsedNameCallable implements Callable<ParsedName> {

    private final String name;
    private final ParsedNameService parsedNameService;
    private final CitationService citationService;

    public ParsedNameCallable(
      String name, ParsedNameService parsedNameService, CitationService citationService
    ) {
      this.name = name;
      this.parsedNameService = parsedNameService;
      this.citationService = citationService;
    }

    @Override
    public ParsedName call() throws Exception {
      for (int x = 0; x < NUM_TASKS; x++) {
        parsedNameService.createOrGet(NameParsers.INSTANCE.parse(name + " " + x + "-banales", null), true);
        citationService.createOrGet(name + " citation #" + x);
      }
      ParsedName pn = parsedNameService.createOrGet(NameParsers.INSTANCE.parse(name, null), true);
      return pn;
    }
  }

  /**
   * Truncate name_usage, name, citation tables.
   */
  private void  truncateTables() throws SQLException {
    LOG.println("Truncate existing data");
    try(Connection cn = dataSource.getConnection();
        PreparedStatement st = cn.prepareStatement("TRUNCATE name_usage, name, citation CASCADE")) {
      cn.setAutoCommit(false);
      st.execute();
      cn.commit();
      cn.setAutoCommit(true);
    }
  }

  @Test
  public void writeNamesInParallel() throws Exception {
    // truncate tables
   truncateTables();
    ExecutorCompletionService<ParsedName> ecs =
      new ExecutorCompletionService<>(Executors.newFixedThreadPool(NUM_THREADS));
    LinkedList<Future<ParsedName>> futures = Lists.newLinkedList();

    for (int i = 0; i < NUM_TASKS; i++) {
      ParsedNameCallable pnc =
        new ParsedNameCallable("Umberto", parsedNameService, citationService);
      LOG.println("Submitting task " + i);
      futures.add(ecs.submit(pnc));
    }

    while (!futures.isEmpty()) {
      Future<ParsedName> f = futures.pop();
      ParsedName pn = f.get();
      if (pn != null) {
        LOG.println(pn.getKey() + " - " + pn.getScientificName());
      } else {
        LOG.println(pn);
      }
    }
    LOG.println("Finished all tasks. Done");
  }
}