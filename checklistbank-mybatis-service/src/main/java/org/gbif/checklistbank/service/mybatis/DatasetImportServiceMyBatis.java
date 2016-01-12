package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;
import org.gbif.checklistbank.service.UsageSyncService;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.checklistbank.utils.ExecutorUtils;
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

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.ibatis.session.ExecutorType;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concurrent import service for full name usages.
 */
public class DatasetImportServiceMyBatis implements DatasetImportService, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(DatasetImportServiceMyBatis.class);

  private static final String NAME = "sync-mybatis";
  private static final int BATCH_SIZE = 1000;

  private final UsageSyncService syncService;
  private ExecutorService exec;
  private ConcurrentLinkedQueue<Future<?>> tasks = new ConcurrentLinkedQueue<>();

  @Inject
  public DatasetImportServiceMyBatis(UsageSyncService importService, @Mybatis Integer threads) {
    this.syncService = importService;
    LOG.info("Starting data import service with {} sync threads.", threads);
    exec = Executors.newFixedThreadPool(threads, new NamedThreadFactory(NAME));
  }

  private Future<Boolean> addTask(Callable<Boolean> task) {
    Future<Boolean> f = exec.submit(task);
    tasks.add(f);
    return f;
  }

  class UsageSync implements Callable<Boolean> {
    final UUID datasetKey;
    final Iterable<Integer> usages;
    final ImporterCallback dao;
    private Map<Integer, Integer> usageKeys;
    private LongSet inserts;
    private int firstId = -1;

    public UsageSync(ImporterCallback dao, UUID datasetKey, Iterable<Integer> usages) {
      this.dao = dao;
      this.datasetKey = datasetKey;
      this.usages = usages;
    }

    @Override
    public Boolean call() throws Exception {
      int counter = 0;
      LOG.debug("Starting usage sync from dataset {}.", datasetKey);
      usageKeys = Maps.newHashMap();
      inserts = new LongHashSet();
      for (List<Integer> batch : Iterables.partition(usages, BATCH_SIZE)) {
        if (firstId < 0) {
          firstId = batch.get(0);
        }
        write(batch);
        counter = counter + batch.size();
      }
      LOG.info("Completed batch of {} usages for dataset {}, starting with id {}.", counter, datasetKey, firstId);

      // submit extension sync job for all usages
      ExtensionSync eSync = new ExtensionSync(dao, datasetKey, usageKeys, inserts);
      dao.reportNewFuture(addTask(eSync));

      return true;
    }

    @Transactional(
        exceptionMessage = "usage sync job failed",
        executorType = ExecutorType.REUSE
    )
    private void write(List<Integer> batch) throws Exception {
      for (Integer id : batch) {
        NameUsage u = dao.readUsage(id);
        NameUsageMetrics m = dao.readMetrics(id);

        boolean insert = dao.isInsert(u);
        if (insert) {
          inserts.add(id);
        }
        syncService.syncUsage(insert, u, m);

        usageKeys.put(id, u.getKey());
        dao.reportUsageKey(id, u.getKey());
      }
    }
  }

  class ProParteSync implements Callable<Boolean> {
    final UUID datasetKey;
    final List<NameUsage> usages;

    public ProParteSync(UUID datasetKey, List<NameUsage> usages) {
      this.datasetKey = datasetKey;
      this.usages = usages;
    }

    @Override
    @Transactional(
        exceptionMessage = "usage sync job failed",
        executorType = ExecutorType.REUSE
    )
    public Boolean call() throws Exception {
      LOG.debug("Starting usage sync with {} usages from dataset {}.", usages.size(), datasetKey);
      for (NameUsage u : usages) {
        // pro parte usages are synonyms and do not have any descendants, synonyms, etc
        NameUsageMetrics m = new NameUsageMetrics();
        m.setKey(u.getKey());
        m.setNumDescendants(0);

        syncService.syncUsage(true, u, m);
      }
      LOG.debug("Completed batch of {} pro parte usages for dataset {}.", usages.size(), datasetKey);
      return true;
    }
  }

  class ExtensionSync implements Callable<Boolean> {
    final UUID datasetKey;
    final Map<Integer, Integer> usages;
    final LongSet inserts;
    final ImporterCallback dao;
    private int firstId = -1;

    public ExtensionSync(ImporterCallback dao, UUID datasetKey, Map<Integer, Integer> usages, LongSet inserts) {
      this.dao = dao;
      this.datasetKey = datasetKey;
      this.usages = usages;
      this.inserts = inserts;
    }

    @Override
    public Boolean call() throws Exception {
      LOG.debug("Starting extension sync for {} usages from dataset {}.", usages.size(), datasetKey);
      for (List<Integer> batch : Iterables.partition(usages.keySet(), BATCH_SIZE)) {
        if (firstId < 0) {
          firstId = batch.get(0);
        }
        write(batch);
      }
      LOG.info("Completed batch of {} usage extensions from dataset {}, starting with id {}.", usages.size(), datasetKey, firstId);
      return true;
    }

    @Transactional(
        exceptionMessage = "extension sync job failed",
        executorType = ExecutorType.REUSE
    )
    private void write(List<Integer> ids) throws Exception {
      for (Integer id : ids) {
        VerbatimNameUsage v = dao.readVerbatim(id);
        UsageExtensions e = dao.readExtensions(id);
        syncService.syncUsageExtras(inserts.contains(id), datasetKey, usages.get(id), v, e);
      }
    }
  }

  class DeletionSync implements Callable<Boolean> {
    final UUID datasetKey;
    final List<Integer> usageKeys;

    public DeletionSync(UUID datasetKey, List<Integer> usageKeys) {
      this.datasetKey = datasetKey;
      this.usageKeys = usageKeys;
    }

    @Override
    public Boolean call() throws Exception {
      LOG.debug("Starting usage deletion for {} usages from dataset {}.", usageKeys.size(), datasetKey);
      for (List<Integer> batch : Lists.partition(usageKeys, BATCH_SIZE)) {
        deleteBatch(batch);
      }
      LOG.debug("Completed batch of {} usage deletions from dataset {}.", usageKeys.size(), datasetKey);
      return true;
    }

    @Transactional(
        exceptionMessage = "usage deletion job failed",
        executorType = ExecutorType.REUSE
    )
    private void deleteBatch(List<Integer> batch) throws Exception {
      for (Integer key : batch) {
        syncService.delete(key);
      }
    }
  }

  class ForeignKeySync implements Callable<Boolean> {
    final List<UsageForeignKeys> fks;

    public ForeignKeySync(List<UsageForeignKeys> fks) {
      this.fks = fks;
    }

    @Override
    public Boolean call() throws Exception {
      LOG.debug("Starting foreign key updates for {} usages.", fks.size());
      for (List<UsageForeignKeys> batch : Lists.partition(fks, BATCH_SIZE)) {
        updateForeignKeyBatch(batch);
      }
      LOG.debug("Completed batch of {} foreign key updates.", fks.size());
      return true;
    }
  }

  @Override
  public Future<Boolean> updateForeignKeys(List<UsageForeignKeys> fks) {
    return exec.submit(new ForeignKeySync(fks));
  }

  @Transactional(
      exceptionMessage = "foreign key update job failed",
      executorType = ExecutorType.REUSE
  )
  private void updateForeignKeyBatch(List<UsageForeignKeys> fks) {
    for (UsageForeignKeys fk : fks) {
      // update usage by usage doing both potential updates in one statement
      syncService.updateForeignKeys(fk.getUsageKey(), fk.getParentKey(), fk.getBasionymKey());
    }
  }

  @Override
  public Future<Boolean> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usages) {
    return addTask(new UsageSync(dao, datasetKey, usages));
  }

  @Override
  public Future<Boolean> sync(UUID datasetKey, List<NameUsage> usages) {
    return addTask(new ProParteSync(datasetKey, usages));
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
  public Future<Boolean> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
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
    ExecutorUtils.stop(exec, NAME, 60, TimeUnit.SECONDS);
  }

}
