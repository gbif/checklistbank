package org.gbif.checklistbank.service.mybatis.tmp;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.NameUsages;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ParsedNameMapper;
import org.gbif.checklistbank.utils.ExecutorUtils;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NameUsageReparser implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(NameUsageReparser.class);
  private static final int BATCH_SIZE = 1000;

  private final NameParser parser = new NameParser();
  private final ExecutorService exec;
  private final NameUsageMapper usageMapper;
  private final ParsedNameMapper nameMapper;

  private int counter = 0;
  private int failed = 0;
  private int unparsable = 0;

  public NameUsageReparser(ClbConfiguration cfg) {
    Injector inj = Guice.createInjector(InternalChecklistBankServiceMyBatisModule.create(cfg));
    nameMapper  = inj.getInstance(ParsedNameMapper.class);
    usageMapper = inj.getInstance(NameUsageMapper.class);
    exec = Executors.newFixedThreadPool(cfg.syncThreads);
  }

  @Override
  public void run() {

    LOG.info("Start reparsing all name usages. This will take a while ...");
    ReparseHandler handler = new ReparseHandler();
    usageMapper.processAllNameUsages(handler);
    // finally submit the remaining unfinished batch
    handler.submitBatch();

    ExecutorUtils.stop(exec, "Reparsing");

    LOG.info("Reparsed {} unique names, {} failed, {} unparsable", counter, failed, unparsable);
  }

  private class ReparseHandler implements ResultHandler<NameUsages> {
    List<NameUsages> batch = Lists.newArrayList();

    @Override
    public void handleResult(ResultContext<? extends NameUsages> context) {
      batch.add(context.getResultObject());
      if (batch.size() >= BATCH_SIZE) {
        submitBatch();
      }
    }

    public void submitBatch() {
      ReparseBatch job = new ReparseBatch(batch);
      exec.submit(job);
      batch.clear();
    }
  }

  class ReparseBatch implements Runnable {
    private final List<NameUsages> names;
    private int counter = 0;
    private int failed = 0;
    private int unparsable = 0;

    public ReparseBatch(List<NameUsages> names) {
      this.names = ImmutableList.copyOf(names);
    }

    class ParsedNameUsage {
      public final ParsedName pn;
      public final Integer[] usageKeys;

      public ParsedNameUsage(ParsedName pn, Integer[] usageKeys) {
        this.pn = pn;
        this.usageKeys = usageKeys;
      }
    }

    @Override
    public void run() {
      // parse names
      List<ParsedNameUsage> pNames = Lists.newArrayList();
      for (NameUsages n : names) {
        counter++;
        pNames.add(new ParsedNameUsage(parse(n), n.getUsageKeys()));
      }

      // write names to table. rank & scientific_name must be unique already!
      writeNames(pNames);

      // update usages
      writeUsages(pNames);

      LOG.info("Reparsed {} unique names, {} failed, {} unparsable", counter, failed, unparsable);
    }

    private ParsedName parse(NameUsages u) {
      ParsedName p;
      try {
        p = parser.parse(u.getScientificName(), u.getRank());

      } catch (UnparsableException e) {
        p = new ParsedName();
        p.setScientificName(u.getScientificName());
        p.setRank(u.getRank());
        p.setType(e.type);

        if (e.type == null || e.type.isParsable()) {
          failed++;
        } else {
          unparsable++;
        }
      }
      return p;
    }

    @Transactional(
        exceptionMessage = "names inserts failed",
        executorType = ExecutorType.REUSE
    )
    private void writeNames(List<ParsedNameUsage> pNames) {
      for (ParsedNameUsage pnu : pNames) {
        nameMapper.create(pnu.pn);
      }
    }

    @Transactional(
        exceptionMessage = "usage updates failed",
        executorType = ExecutorType.REUSE
    )
    private void writeUsages(List<ParsedNameUsage> pNames) {
      for (ParsedNameUsage pnu : pNames) {
        for (Integer usageKey : pnu.usageKeys) {
          usageMapper.updateName(usageKey, pnu.pn.getKey());
        }
      }
    }
  }
}
