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
package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.model.SolrUsage;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.utils.concurrent.ExecutorUtils;
import org.gbif.utils.concurrent.NamedThreadFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Meter;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Service that updates a solr checklistbank index in real time.
 * A maximum of one minute is allowed for a commit to happen.
 */
@Service
public class NameUsageIndexServiceSolr implements DatasetImportService {

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageIndexServiceSolr.class);
  private final static String NAME = "sync-solr";

  private final NameUsageDocConverter converter = new NameUsageDocConverter();
  private final SolrClient solr;
  private final int batchSize = 25;
  private final UsageService usageService;
  private final VernacularNameService vernacularNameService;
  private final DescriptionService descriptionService;
  private final DistributionService distributionService;
  private final SpeciesProfileService speciesProfileService;
  // consider only some extension records at most
  private final PagingRequest page = new PagingRequest(0, 500);
  private final ExecutorService exec;
  private ConcurrentLinkedQueue<Future<?>> tasks = new ConcurrentLinkedQueue<>();
  private final Meter updMeter = new Meter();
  private final AtomicInteger updCounter = new AtomicInteger(0);

  @Autowired
  public NameUsageIndexServiceSolr(
    SolrClient solr,
    UsageService usageService,
    VernacularNameService vernacularNameService,
    DescriptionService descriptionService,
    DistributionService distributionService,
    SpeciesProfileService speciesProfileService,
    @Qualifier("syncThreads") Integer syncThreads
  ) {
    this.solr = solr;
    this.usageService = usageService;
    this.vernacularNameService = vernacularNameService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
    this.speciesProfileService = speciesProfileService;

    exec = Executors.newFixedThreadPool(syncThreads, new NamedThreadFactory(NAME));
  }

  private <T> Future<T> addTask(Callable<T> task) {
    Future<T> f = exec.submit(task);
    tasks.add(f);
    return f;
  }

  private void insertOrUpdateByKey(Iterable<Integer> keys) {
    insertOrUpdate(Iterables.transform(keys, new Function<Integer, SolrUsage>() {
      @Nullable
      @Override
      public SolrUsage apply(Integer id) {
        int key = id;
        // we use the list service for just one record cause its more effective
        // leaving out fields that we do not index in solr
        List<NameUsage> range = usageService.listRange(key, key);
        if (range.isEmpty()) {
          return null;

        } else {
          NameUsage u = range.get(0);
          UsageExtensions ext = new UsageExtensions();
          ext.distributions = distributionService.listByUsage(key, page).getResults();
          ext.descriptions = descriptionService.listByUsage(key, page).getResults();
          ext.vernacularNames = vernacularNameService.listByUsage(key, page).getResults();
          ext.speciesProfiles = speciesProfileService.listByUsage(key, page).getResults();
          return new SolrUsage(u, usageService.listParents(key), ext);
        }
      }
    }));
  }


  public void insertOrUpdate(Iterable<SolrUsage> usages) {
    UUID datasetKey = null;
    for (Iterable<SolrUsage> batch : Iterables.partition(usages, batchSize)) {
      List<SolrInputDocument> docs = Lists.newArrayList();
      for (SolrUsage u : batch) {
        if (u == null) continue;

        if (datasetKey==null) {
          datasetKey=u.usage.getDatasetKey();
        }
        docs.add(converter.toDoc(u.usage, u.parents, u.extensions));
      }
      try {
        if (!docs.isEmpty()) {
          solr.add(docs);
          updMeter.mark();
          int cnt = updCounter.incrementAndGet();
          if (cnt % 10000 == 0) {
            LogContext.startDataset(datasetKey);
            LOG.info("Synced {} usages, mean rate={}", cnt, updMeter.getMeanRate());
            LogContext.endDataset();
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Future<List<Integer>> updateForeignKeys(UUID datasetKey, List<UsageForeignKeys> fks) {
    List<Integer> usageKeys = Lists.newArrayList();
    for (UsageForeignKeys fk : fks) {
      usageKeys.add(fk.getUsageKey());
    }
    return addTask(new SolrUpdateMybatis(usageKeys));
  }

  @Override
  public Future<List<Integer>> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usageNeoIds) {
    return addTask(new SolrUpdateCallback(dao, usageNeoIds));
  }

  /**
   * @param names list of names being ignored. Can be null!
   */
  @Override
  public Future<List<NameUsage>> sync(UUID datasetKey, ImporterCallback dao, List<NameUsage> usages, @Nullable List<ParsedName> names) {
    return addTask(new SolrUpdateProParte(usages));
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    exec.submit(new SolrUpdateMybatis(Lists.<Integer>newArrayList(relations.keySet())));
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    try {
      solr.deleteByQuery("dataset_key:"+datasetKey.toString());
      return 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Future<List<Integer>> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
    return addTask(new SolrDelete(usageKeys));
  }

  @Override
  public boolean isRunning() {
    Iterator<Future<?>> iter = tasks.iterator();
    while(iter.hasNext()) {
      Future<?> f = iter.next();
      if (f.isDone()) {
        iter.remove();
      } else {
        return true;
      }
    }
    return false;
  }

  @Override
  public void close() throws Exception {
    ExecutorUtils.stop(exec, 60, TimeUnit.SECONDS);
  }

  class SolrUpdateProParte implements Callable<List<NameUsage>> {
    private final List<NameUsage> usages;

    public SolrUpdateProParte(List<NameUsage> usages) {
      this.usages = usages;
    }

    @Override
    public List<NameUsage> call() throws Exception {
      insertOrUpdate(Lists.transform(usages, new Function<NameUsage, SolrUsage>() {
        @Override
        public SolrUsage apply(NameUsage u) {
          // the pro parte usage itself might not yet be synced...
          // so we get list of parent ids from parent which must exist in postgres already!
          List<Integer> parents = Lists.newArrayList();
          if (u.getAcceptedKey() != null) {
            parents.add(u.getAcceptedKey());
            parents.addAll(usageService.listParents(u.getAcceptedKey()));
          } else if (u.getParentKey() != null) {
            parents.add(u.getParentKey());
            parents.addAll(usageService.listParents(u.getParentKey()));
          }
          return new SolrUsage(u, parents,null);
        }
      }));
      return usages;
    }
  }

  /**
   * Updates solr by loading given usage keys from the provided ImporterCallback handler.
   * This is used by the neo4j backed indexing tools to provide data from neo without coupling the service here to neo4j.
   */
  class SolrUpdateCallback implements Callable<List<Integer>> {
    private final Iterable<Integer> usages;
    private final ImporterCallback dao;

    /**
     * @param usages usage keys as required by the callback service (usually neo4j ids, NOT postgres usage keys)
     */
    public SolrUpdateCallback(ImporterCallback dao, Iterable<Integer> usages) {
      this.dao = dao;
      this.usages = usages;
    }

    @Override
    public List<Integer> call() throws Exception {
      final List<Integer> ids = Lists.newArrayList();
      insertOrUpdate(Iterables.transform(usages, new Function<Integer, SolrUsage>() {
        @Override
        public SolrUsage apply(Integer id) {
          ids.add(id);
          NameUsage u = dao.readUsage(id);
          UsageExtensions e = dao.readExtensions(id);
          return new SolrUsage(u, usageService.listParents(u.getKey()), e);
        }
      }));
      return ids;
    }
  }

  /**
   * Updates solr by loading given usage keys from mybatis.
   */
  class SolrUpdateMybatis implements Callable<List<Integer>> {
    private final List<Integer> ids;

    public SolrUpdateMybatis(List<Integer> ids) {
      this.ids = ids;
    }

    @Override
    public List<Integer> call() throws Exception {
      insertOrUpdateByKey(ids);
      return ids;
    }
  }

  class SolrDelete implements Callable<List<Integer>> {
    private final List<Integer> ids;

    public SolrDelete(List<Integer> ids) {
      this.ids = ids;
    }

    @Override
    public List<Integer> call() throws Exception {
      if (!ids.isEmpty()) {
        LOG.info("Deleting {} usages from solr", ids.size());
        List<String> idsAsStrings = ids.stream().map(Object::toString).collect(Collectors.toList());
        solr.deleteById(idsAsStrings);
      }
      return ids;
    }
  }
}
