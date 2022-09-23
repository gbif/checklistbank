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
import org.gbif.checklistbank.index.model.NameUsageAvro;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.utils.concurrent.ExecutorUtils;
import org.gbif.utils.concurrent.NamedThreadFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Service that updates an Elasticsearch Checklistbank index in real time.
 * A maximum of one minute is allowed for a commit to happen.
 */
@Service
@Slf4j
public class NameUsageIndexServiceEs implements DatasetImportService {

  private final static String NAME = "sync-elasticsearch";

  private final NameUsagesEsIndexingClient esClient;
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

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired
  public NameUsageIndexServiceEs(
    ElasticsearchClient elasticsearchClient,
    UsageService usageService,
    VernacularNameService vernacularNameService,
    DescriptionService descriptionService,
    DistributionService distributionService,
    SpeciesProfileService speciesProfileService,
    @Qualifier("syncThreads") Integer syncThreads,
    @Qualifier("indexName") String indexName
  ) {
    this.esClient = NameUsagesEsIndexingClient.builder().elasticsearchClient(elasticsearchClient).indexName(indexName).build();
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
    insertOrUpdate(StreamSupport.stream(keys.spliterator(), false).map(key -> {
        // we use the list service for just one record cause its more effective
        // leaving out fields that we do not index in the index
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
          return NameUsageAvroConverter.toObject(u, usageService.listParents(key), ext);
        }
      }
    ).collect(Collectors.toList()));
  }

  private static  <T> Collection<List<T>> partition(Iterable<T> iterable, int batchSize) {
    final AtomicInteger counter = new AtomicInteger();
    return StreamSupport.stream(iterable.spliterator(),true)
              .filter(Objects::nonNull)
              .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / batchSize))
              .values();
  }

  @SneakyThrows
  public void insertOrUpdate(Iterable<NameUsageAvro> usages) {
    for (List<NameUsageAvro> batch : partition(usages, batchSize)) {
      if (!batch.isEmpty()) {
        String datasetKey =  batch.get(0).getDatasetKey();
        esClient.bulkAdd(batch);
        updMeter.mark();
        int cnt = updCounter.incrementAndGet();
        if (cnt % 10000 == 0) {
          LogContext.startDataset(datasetKey);
          log.info("Synced {} usages, mean rate={}", cnt, updMeter.getMeanRate());
          LogContext.endDataset();
        }
      }
    }
  }

  @Override
  public Future<List<Integer>> updateForeignKeys(UUID datasetKey, List<UsageForeignKeys> fks) {
    List<Integer> usageKeys = new ArrayList<>();
    for (UsageForeignKeys fk : fks) {
      usageKeys.add(fk.getUsageKey());
    }
    return addTask(new IndexUpdateMybatis(usageKeys));
  }

  @Override
  public Future<List<Integer>> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usageNeoIds) {
    return addTask(new IndexUpdateCallback(dao, usageNeoIds));
  }

  /**
   * @param names list of names being ignored. Can be null!
   */
  @Override
  public Future<List<NameUsage>> sync(UUID datasetKey, ImporterCallback dao, List<NameUsage> usages, @Nullable List<ParsedName> names) {
    return addTask(new IndexUpdateProParte(usages));
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    exec.submit(new IndexUpdateMybatis(new ArrayList<>(relations.keySet())));
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    try {
      esClient.deleteByDatasetKey(datasetKey);
      return 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Future<List<Integer>> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
    return addTask(new IndexDelete(usageKeys));
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

  class IndexUpdateProParte implements Callable<List<NameUsage>> {
    private final List<NameUsage> usages;

    public IndexUpdateProParte(List<NameUsage> usages) {
      this.usages = usages;
    }

    @Override
    public List<NameUsage> call() throws Exception {
      insertOrUpdate(usages.stream().map(u -> {
          // the pro parte usage itself might not yet be synced...
          // so we get list of parent ids from parent which must exist in postgres already!
          List<Integer> parents = new ArrayList<>();
          if (u.getAcceptedKey() != null) {
            parents.add(u.getAcceptedKey());
            parents.addAll(usageService.listParents(u.getAcceptedKey()));
          } else if (u.getParentKey() != null) {
            parents.add(u.getParentKey());
            parents.addAll(usageService.listParents(u.getParentKey()));
          }
          return NameUsageAvroConverter.toObject(u, parents,null);
      }).collect(Collectors.toList()));
      return usages;
    }
  }

  /**
   * Updates the search index by loading given usage keys from the provided ImporterCallback handler.
   * This is used by the neo4j backed indexing tools to provide data from neo without coupling the service here to neo4j.
   */
  class IndexUpdateCallback implements Callable<List<Integer>> {
    private final Iterable<Integer> usages;
    private final ImporterCallback dao;

    /**
     * @param usages usage keys as required by the callback service (usually neo4j ids, NOT postgres usage keys)
     */
    public IndexUpdateCallback(ImporterCallback dao, Iterable<Integer> usages) {
      this.dao = dao;
      this.usages = usages;
    }

    @Override
    public List<Integer> call() throws Exception {
      final List<Integer> ids = new ArrayList<>();
      insertOrUpdate(StreamSupport.stream(usages.spliterator(), false)
                       .map(id -> {
                                    ids.add(id);
                                    NameUsage u = dao.readUsage(id);
                                    UsageExtensions e = dao.readExtensions(id);
                                    return NameUsageAvroConverter.toObject(u, usageService.listParents(u.getKey()), e);
                                  })
                       .collect(Collectors.toList()));
      return ids;
    }
  }

  /**
   * Updates the search index by loading given usage keys from mybatis.
   */
  class IndexUpdateMybatis implements Callable<List<Integer>> {
    private final List<Integer> ids;

    public IndexUpdateMybatis(List<Integer> ids) {
      this.ids = ids;
    }

    @Override
    public List<Integer> call() throws Exception {
      insertOrUpdateByKey(ids);
      return ids;
    }
  }

  class IndexDelete implements Callable<List<Integer>> {
    private final List<Integer> ids;

    public IndexDelete(List<Integer> ids) {
      this.ids = ids;
    }

    @Override
    public List<Integer> call() throws Exception {
      if (!ids.isEmpty()) {
        log.info("Deleting {} usages from elasticsearch", ids.size());
        esClient.bulkDelete(ids);
      }
      return ids;
    }
  }
}
