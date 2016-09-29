package org.gbif.checklistbank.service.mybatis.tmp;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.ScientificName;
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
  private final int threads;

  private int jobCounter = 0;
  private int counter = 0;
  private int failed = 0;
  private int unparsable = 0;

  public NameUsageReparser(ClbConfiguration cfg) {
    Injector inj = Guice.createInjector(InternalChecklistBankServiceMyBatisModule.create(cfg));
    nameMapper  = inj.getInstance(ParsedNameMapper.class);
    usageMapper = inj.getInstance(NameUsageMapper.class);
    threads = Math.max(1, cfg.maximumPoolSize-1);
    exec = Executors.newFixedThreadPool(threads);
  }

  @Override
  public void run() {
    LOG.info("Submit reparsing jobs in batches of {} to executor with {} threads.", BATCH_SIZE, threads);
    ReparseHandler handler = new ReparseHandler();
    usageMapper.processAllNameUsages(handler);
    // finally submit the remaining unfinished batch
    handler.submitBatch();

    LOG.info("Submitted all {} jobs.", jobCounter);

    ExecutorUtils.stop(exec, "Reparsing");

    if (jobCounter != 0) {
      LOG.warn("Something not right. All jobs should be done but {} remain in counter", jobCounter);
    }
    LOG.info("Done! Reparsed {} unique names, {} failed, {} unparsable", counter, failed, unparsable);
  }

  private class ReparseHandler implements ResultHandler<ScientificName> {
    List<ScientificName> batch = Lists.newArrayList();

    @Override
    public void handleResult(ResultContext<? extends ScientificName> context) {
      batch.add(context.getResultObject());
      if (batch.size() >= BATCH_SIZE) {
        submitBatch();
      }
    }

    public void submitBatch() {
      ReparseBatch job = new ReparseBatch(batch);
      exec.submit(job);
      batch.clear();
      jobCounter++;
    }
  }

  private class ReparseBatch implements Runnable {
    private final List<ScientificName> names;

    private ReparseBatch(List<ScientificName> names) {
      this.names = ImmutableList.copyOf(names);
    }

    @Override
    public void run() {
      try {
        // parse names
        List<ParsedName> pNames = Lists.newArrayList();
        for (ScientificName n : names) {
          counter++;
          pNames.add(parse(n));
        }

        // write names to table. rank & scientific_name must be unique already!
        writeNames(pNames);

        jobCounter--;
        if (jobCounter % 100 == 0) {
          LOG.info("Reparsed {} unique names in {} batches, {} failed, {} unparsable", counter, jobCounter, failed, unparsable);
        } else if (jobCounter % 10 == 0) {
          LOG.debug("Reparsed {} unique names in {} batches, {} failed, {} unparsable", counter, jobCounter, failed, unparsable);
        }

      } catch (Exception e) {
        LOG.error("Batch reparsing error {}", e);
      }
    }

    private ParsedName parse(ScientificName u) {
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

      } catch (Exception e) {
        LOG.error("Parsing error for {} {}: ", u.getRank(), u.getScientificName(), e);

        p = new ParsedName();
        p.setScientificName(u.getScientificName());
        p.setRank(u.getRank());
        p.setType(NameType.DOUBTFUL);
        p.setRemarks("parsing failure");

        failed++;
      }

      return p;
    }

    @Transactional(
        exceptionMessage = "names inserts failed",
        executorType = ExecutorType.REUSE
    )
    private void writeNames(List<ParsedName> pNames) {
      for (ParsedName pn : pNames) {
        try {
          nameMapper.create(pn);
        } catch (Exception e) {
          LOG.error("Error persisting name: {}", pn, e);
        }
      }
    }
  }
  private static ParsedName parse(NameParser parser, ScientificName u) {
    ParsedName p;
    try {
      p = parser.parse(u.getScientificName(), u.getRank());

    } catch (UnparsableException e) {
      p = new ParsedName();
      p.setScientificName(u.getScientificName());
      p.setRank(u.getRank());
      p.setType(e.type);

    } catch (Exception e) {
      LOG.error("Parsing error for {} {}: ", u.getRank(), u.getScientificName(), e);

      p = new ParsedName();
      p.setScientificName(u.getScientificName());
      p.setRank(u.getRank());
      p.setType(NameType.DOUBTFUL);
      p.setRemarks("parsing failure");
    }

    return p;
  }

  public static void main (String[] args) {
    NameParser parser = new NameParser();
    System.out.println(parse(parser, new ScientificName(0, "Taraxacum erythrospermum agg.", Rank.SECTION)));

  }
}
