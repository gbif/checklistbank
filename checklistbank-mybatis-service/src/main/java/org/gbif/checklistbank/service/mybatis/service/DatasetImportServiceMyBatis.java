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

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;
import org.gbif.checklistbank.service.UsageSyncService;
import org.gbif.utils.concurrent.ExecutorUtils;
import org.gbif.utils.concurrent.NamedThreadFactory;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

/**
 * Concurrent import service for full name usages.
 */
@Service
public class DatasetImportServiceMyBatis implements DatasetImportService, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(DatasetImportServiceMyBatis.class);

  private static final String NAME = "sync-mybatis";
  private static final int BATCH_SIZE = 1000;

  private final UsageSyncService syncService;
  private ExecutorService exec;
  private ConcurrentLinkedQueue<Future<?>> tasks = new ConcurrentLinkedQueue<>();

  @Autowired
  public DatasetImportServiceMyBatis(UsageSyncService importService, ClbConfiguration cfg) {
    this.syncService = importService;
    LOG.info("Starting data import service with {} sync threads.", cfg.syncThreads);
    exec = Executors.newFixedThreadPool(cfg.syncThreads, new NamedThreadFactory(NAME));
  }

  private <T> Future<T> addTask(Callable<T> task) {
    Future<T> f = exec.submit(task);
    tasks.add(f);
    return f;
  }

  class UsageSync implements Callable<List<Integer>> {
    final UUID datasetKey;
    final Iterable<Integer> usages;
    final ImporterCallback dao;
    private Map<Integer, Integer> usageKeys;
    private Set<Integer> inserts;
    private int firstId = -1;

    /**
     * @param dao callback to importer neo4j dao to resolve neo4j ids
     * @param datasetKey
     * @param usages list of neo4j node ids to sync from callback
     */
    public UsageSync(ImporterCallback dao, UUID datasetKey, Iterable<Integer> usages) {
      this.dao = dao;
      this.datasetKey = datasetKey;
      this.usages = usages;
    }

    @Override
    public List<Integer> call() throws Exception {
      LogContext.startDataset(datasetKey);
      int counter = 0;
      LOG.debug("Starting usage sync");
      usageKeys = Maps.newHashMap();
      inserts = new IntOpenHashSet();
      List<Integer> neoKeys = Lists.newArrayList();
      for (List<Integer> neoBatch : Iterables.partition(usages, BATCH_SIZE)) {
        if (firstId < 0) {
          firstId = neoBatch.get(0);
        }
        neoKeys.addAll(neoBatch);
        write(neoBatch);
        counter = counter + neoBatch.size();
      }
      LOG.info("Completed batch of {} usages, starting with id {}.", counter, firstId);
      LogContext.endDataset();

      // submit extension sync job for all usages
      ExtensionSync eSync = new ExtensionSync(dao, datasetKey, firstId, usageKeys, inserts);
      dao.reportNewFuture(addTask(eSync));

      return neoKeys;
    }

    @Transactional
    private void write(List<Integer> neoNodeIdBatch) throws Exception {
      for (Integer id : neoNodeIdBatch) {
        NameUsage u = dao.readUsage(id);
        ParsedName pn = dao.readName(id);
        NameUsageMetrics m = dao.readMetrics(id);

        boolean insert = dao.isInsert(u);
        syncService.syncUsage(insert, u, pn, m);

        // remember usageKey and things about this record
        if (insert) {
          inserts.add(id);
        }
        usageKeys.put(id, u.getKey());
        // tell main importer about the new usageKey so we can prepare usages with good foreign keys
        dao.reportUsageKey(id, u.getKey());
      }
    }
  }

  class ProParteSync implements Callable<List<NameUsage>> {
    final ImporterCallback dao;
    final UUID datasetKey;
    final List<NameUsage> usages;
    final List<ParsedName> names;

    public ProParteSync(ImporterCallback dao, UUID datasetKey, List<NameUsage> usages, List<ParsedName> names) {
      this.dao = dao;
      this.datasetKey = datasetKey;
      this.usages = usages;
      this.names = names;
      Preconditions.checkArgument(usages.size() == names.size());
    }

    @Override
    @Transactional
    public List<NameUsage> call() throws Exception {
      LogContext.startDataset(datasetKey);
      LOG.debug("Starting usage sync with {} usages", usages.size());
      Iterator<ParsedName> nIter = names.iterator();
      for (NameUsage u : usages) {
        // pro parte usages are synonyms and do not have any descendants, synonyms, etc
        NameUsageMetrics m = new NameUsageMetrics();
        ParsedName pn = nIter.next();
        m.setKey(u.getKey());
        m.setNumDescendants(0);

        boolean insert = dao.isInsert(u);
        syncService.syncUsage(insert, u, pn, m);
      }
      LOG.debug("Completed batch of {} pro parte usages", usages.size());
      LogContext.endDataset();
      return usages;
    }
  }

  class ExtensionSync implements Callable<List<Integer>> {
    final UUID datasetKey;
    final Map<Integer, Integer> usages;
    final Set<Integer> inserts;
    final ImporterCallback dao;
    private int firstId = -1;

    public ExtensionSync(ImporterCallback dao, UUID datasetKey, int firstId, Map<Integer, Integer> usages, Set<Integer> inserts) {
      this.dao = dao;
      this.datasetKey = datasetKey;
      this.usages = usages;
      this.inserts = inserts;
      this.firstId = firstId;
    }

    @Override
    public List<Integer> call() throws Exception {
      LogContext.startDataset(datasetKey);
      LOG.debug("Starting extension sync for {} usages", usages.size());
      List<Integer> ids = Lists.newArrayList();
      for (List<Integer> batch : Iterables.partition(usages.keySet(), BATCH_SIZE)) {
        write(batch);
        ids.addAll(batch);
      }
      LOG.info("Completed batch of {} usage extensions, starting with id {}.", usages.size(), firstId);
      LogContext.endDataset();
      return ids;
    }

    @Transactional
    private void write(List<Integer> ids) throws Exception {
      for (Integer id : ids) {
        VerbatimNameUsage v = dao.readVerbatim(id);
        UsageExtensions e = dao.readExtensions(id);
        syncService.syncUsageExtras(inserts.contains(id), datasetKey, usages.get(id), v, e);
      }
    }
  }

  class DeletionSync implements Callable<List<Integer>> {
    final UUID datasetKey;
    final List<Integer> usageKeys;

    public DeletionSync(UUID datasetKey, List<Integer> usageKeys) {
      this.datasetKey = datasetKey;
      this.usageKeys = usageKeys;
    }

    @Override
    public List<Integer> call() throws Exception {
      LogContext.startDataset(datasetKey);
      LOG.info("Starting deletion for {} usages", usageKeys.size());
      for (List<Integer> batch : Lists.partition(usageKeys, BATCH_SIZE)) {
        deleteBatch(batch);
      }
      LOG.debug("Completed batch of {} usage deletions", usageKeys.size());
      LogContext.endDataset();
      return usageKeys;
    }

    @Transactional
    private void deleteBatch(List<Integer> batch) throws Exception {
      for (Integer key : batch) {
        syncService.delete(key);
      }
    }
  }

  class ForeignKeySync implements Callable<List<Integer>> {
    final List<UsageForeignKeys> fks;
    final UUID datasetKey;

    public ForeignKeySync(UUID datasetKey, List<UsageForeignKeys> fks) {
      this.fks = fks;
      this.datasetKey = datasetKey;
    }

    @Override
    public List<Integer> call() throws Exception {
      LogContext.startDataset(datasetKey);
      LOG.debug("Starting foreign key updates for {} usages.", fks.size());
      List<Integer> ids = Lists.newArrayList();
      for (List<UsageForeignKeys> batch : Lists.partition(fks, BATCH_SIZE)) {
        ids.addAll(updateForeignKeyBatch(batch));
      }
      LOG.debug("Completed batch of {} foreign key updates.", fks.size());
      LogContext.endDataset();
      return ids;
    }
  }

  @Override
  public Future<List<Integer>> updateForeignKeys(UUID datasetKey, List<UsageForeignKeys> fks) {
    return exec.submit(new ForeignKeySync(datasetKey, fks));
  }

  @Transactional
  private List<Integer> updateForeignKeyBatch(List<UsageForeignKeys> fks) {
    List<Integer> ids = Lists.newArrayList();
    for (UsageForeignKeys fk : fks) {
      // update usage by usage doing both potential updates in one statement
      syncService.updateForeignKeys(fk.getUsageKey(), fk.getParentKey(), fk.getBasionymKey());
      ids.add(fk.getUsageKey());
    }
    return ids;
  }

  @Override
  public Future<List<Integer>> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usageNeoIds) {
    return addTask(new UsageSync(dao, datasetKey, usageNeoIds));
  }

  @Override
  public Future<List<NameUsage>> sync(UUID datasetKey, ImporterCallback dao, List<NameUsage> usages, List<ParsedName> names) {
    return addTask(new ProParteSync(dao, datasetKey, usages, names));
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    syncService.insertNubRelations(datasetKey, relations);
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    return syncService.deleteDataset(datasetKey);
  }

  @Override
  public Future<List<Integer>> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
    return addTask(new DeletionSync(datasetKey, usageKeys));
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

}
