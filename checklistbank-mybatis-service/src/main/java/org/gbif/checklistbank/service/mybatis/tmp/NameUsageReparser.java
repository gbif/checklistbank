package org.gbif.checklistbank.service.mybatis.tmp;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.ScientificName;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ParsedNameMapper;
import org.gbif.nameparser.GBIFNameParser;
import org.gbif.utils.concurrent.ExecutorUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ibatis.exceptions.PersistenceException;
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

  private final NameParser parser = new GBIFNameParser();
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

    ExecutorUtils.stop(exec, 10, TimeUnit.SECONDS);

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

  class ScientificParsedName {
    public final ScientificName sciname;
    public final ParsedName pn;

    public ScientificParsedName(ScientificName sciname, ParsedName pn) {
      this.sciname = sciname;
      this.pn = pn;
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
        List<ScientificParsedName> pNames = Lists.newArrayList();
        for (ScientificName n : names) {
          counter++;
          ParsedName p = parser.parseQuietly(n.getScientificName(), n.getRank());
          if (!p.isParsed()) {
            if (p.getType() == null || p.getType().isParsable()) {
              failed++;
            } else {
              unparsable++;
            }
          }
          pNames.add(new ScientificParsedName(n, p));
        }

        // write names to table. rank & scientific_name must be unique already!
        writeNames(pNames);

        jobCounter--;
        if (jobCounter % 100 == 0) {
          LOG.info("Reparsed {} unique names. {} failed, {} unparsable. {} batches left", counter, failed, unparsable, jobCounter);
        } else if (jobCounter % 10 == 0) {
          LOG.debug("Reparsed {} unique names. {} failed, {} unparsable. {} batches left", counter, failed, unparsable, jobCounter);
        }

      } catch (Exception e) {
        LOG.error("Batch reparsing error {}", e);
      }
    }

    @Transactional(
        exceptionMessage = "names inserts failed",
        executorType = ExecutorType.REUSE
    )
    private void writeNames(List<ScientificParsedName> pNames) {
      for (ScientificParsedName spn : pNames) {
        try {
          nameMapper.create2(spn.sciname.getKey(), spn.pn);
        } catch (PersistenceException e) {
          Throwable cause = e.getCause() != null ? e.getCause() : e;
          LOG.warn("Failed to persist name {}: {}", spn.pn, cause.getMessage());
          nameMapper.failed(spn.sciname.getKey(), spn.pn.getScientificName(), spn.pn.getRank());

        } catch (Exception e) {
          LOG.error("Unexpected error persisting name {}", spn.pn, e);
          nameMapper.failed(spn.sciname.getKey(), spn.sciname.getScientificName(), spn.sciname.getRank());
        }
      }
    }
  }

}
