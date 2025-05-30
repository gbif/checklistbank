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
package org.gbif.checklistbank.service.mybatis.tmp;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.v2.RankedName;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.ParsedNameMapper;
import org.gbif.checklistbank.utils.NameParsers;
import org.gbif.utils.concurrent.ExecutorUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class NameUsageReparser implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(NameUsageReparser.class);
  private static final int BATCH_SIZE = 1000;

  private final ExecutorService exec;
  private final NameUsageMapper usageMapper;
  private final ParsedNameMapper nameMapper;
  private final int threads;

  private int jobCounter = 0;
  private int counter = 0;
  private int failed = 0;
  private int unparsable = 0;

  public NameUsageReparser(ClbConfiguration cfg, NameUsageMapper usageMapper, ParsedNameMapper nameMapper) {
    this.usageMapper = usageMapper;
    this.nameMapper = nameMapper;
    threads = Math.max(1, cfg.maximumPoolSize - 1);
    exec = Executors.newFixedThreadPool(threads);
  }

  @Transactional
  @Override
  public void run() {
    LOG.info(
        "Submit reparsing jobs in batches of {} to executor with {} threads.", BATCH_SIZE, threads);
    ReparseHandler handler = new ReparseHandler();
    usageMapper.processAllNames().forEach(handler);
    // finally submit the remaining unfinished batch
    handler.submitBatch();

    LOG.info("Submitted all {} jobs.", jobCounter);

    ExecutorUtils.stop(exec, 10, TimeUnit.SECONDS);

    if (jobCounter != 0) {
      LOG.warn("Something not right. All jobs should be done but {} remain in counter", jobCounter);
    }
    LOG.info(
        "Done! Reparsed {} unique names, {} failed, {} unparsable", counter, failed, unparsable);
  }

  private class ReparseHandler implements Consumer<RankedName> {
    List<RankedName> batch = Lists.newArrayList();

    @Override
    public void accept(RankedName result) {
      batch.add(result);
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
    public final RankedName sciname;
    public final ParsedName pn;

    public ScientificParsedName(RankedName sciname, ParsedName pn) {
      this.sciname = sciname;
      this.pn = pn;
    }
  }

  private class ReparseBatch implements Runnable {
    private final List<RankedName> names;

    private ReparseBatch(List<RankedName> names) {
      this.names = ImmutableList.copyOf(names);
    }

    @Override
    public void run() {
      try {
        // parse names
        List<ScientificParsedName> pNames = Lists.newArrayList();
        for (RankedName n : names) {
          counter++;
          ParsedName p = NameParsers.INSTANCE.parseQuietly(n.getName(), n.getRank());
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
          LOG.info(
              "Reparsed {} unique names. {} failed, {} unparsable. {} batches left",
              counter,
              failed,
              unparsable,
              jobCounter);
        } else if (jobCounter % 10 == 0) {
          LOG.debug(
              "Reparsed {} unique names. {} failed, {} unparsable. {} batches left",
              counter,
              failed,
              unparsable,
              jobCounter);
        }

      } catch (Exception e) {
        LOG.error("Batch reparsing error {}", e);
      }
    }

    @Transactional
    private void writeNames(List<ScientificParsedName> pNames) {
      for (ScientificParsedName spn : pNames) {
        try {
          nameMapper.createWithKey(spn.sciname.getKey(), spn.pn);
        } catch (DataIntegrityViolationException e) {
          Throwable cause = e.getCause() != null ? e.getCause() : e;
          LOG.warn("Failed to persist name {}: {}", spn.pn, cause.getMessage());
          nameMapper.failed(spn.sciname.getKey(), spn.pn.getScientificName(), spn.pn.getRank());

        } catch (Exception e) {
          LOG.error("Unexpected error persisting name {}", spn.pn, e);
          nameMapper.failed(spn.sciname.getKey(), spn.sciname.getName(), spn.sciname.getRank());
        }
      }
    }
  }
}
