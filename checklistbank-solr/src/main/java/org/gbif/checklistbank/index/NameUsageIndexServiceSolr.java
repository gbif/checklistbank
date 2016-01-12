package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.utils.ExecutorUtils;
import org.gbif.utils.concurrent.NamedThreadFactory;

import java.util.Collection;
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
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.yammer.metrics.Meter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that updates a solr checklistbank index in real time.
 * A maximum of one minute is allowed for a commit to happen.
 */
public class NameUsageIndexServiceSolr implements DatasetImportService {

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageIndexServiceSolr.class);
  private final static String NAME = "sync-solr";

  private final NameUsageDocConverter converter = new NameUsageDocConverter();
  private final SolrClient solr;
  private final int commitWithinMs = 60*1000;
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
  @Inject
  public NameUsageIndexServiceSolr(
    SolrClient solr,
    UsageService usageService,
    VernacularNameService vernacularNameService,
    DescriptionService descriptionService,
    DistributionService distributionService,
    SpeciesProfileService speciesProfileService,
    @Solr Integer syncThreads
  ) {
    this.solr = solr;
    this.usageService = usageService;
    this.vernacularNameService = vernacularNameService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
    this.speciesProfileService = speciesProfileService;

    exec = Executors.newFixedThreadPool(syncThreads, new NamedThreadFactory(NAME));
  }

  private Future<Boolean> addTask(Callable<Boolean> task) {
    Future<Boolean> f = exec.submit(task);
    tasks.add(f);
    return f;
  }

  private void insertOrUpdate(int key) {
    // we use the list service for just one record cause its more effective
    // leaving out fields that we do not index in solr
    List<NameUsage> range = usageService.listRange(key, key);
    if (!range.isEmpty()) {
      NameUsage u = range.get(0);
      UsageExtensions ext = new UsageExtensions();
      ext.distributions = distributionService.listByUsage(key, page).getResults();
      ext.descriptions = descriptionService.listByUsage(key, page).getResults();
      ext.vernacularNames = vernacularNameService.listByUsage(key, page).getResults();
      ext.speciesProfiles = speciesProfileService.listByUsage(key, page).getResults();

      insertOrUpdate(u, usageService.listParents(key), ext);
    }
  }

  private void insertOrUpdate(NameUsage usage, List<Integer> parentKeys, @Nullable UsageExtensions extensions) {
    SolrInputDocument doc = converter.toObject(usage, parentKeys, extensions);
    try {
      solr.add(doc, commitWithinMs);
      updMeter.mark();
      int cnt = updCounter.incrementAndGet();
      if (cnt % 10000 == 0) {
        LOG.info("Synced {} usages, mean rate={}", cnt, updMeter.getMeanRate());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Future<Boolean> updateForeignKeys(List<UsageForeignKeys> fks) {
    List<Integer> usageKeys = Lists.newArrayList();
    for (UsageForeignKeys fk : fks) {
      usageKeys.add(fk.getUsageKey());
    }
    return addTask(new SolrUpdateMybatis(usageKeys));
  }

  @Override
  public Future<Boolean> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usages) {
    return addTask(new SolrUpdateCallback(dao, usages));
  }

  @Override
  public Future<Boolean> sync(UUID datasetKey, List<NameUsage> usages, List<ParsedName> names) {
    return addTask(new SolrUpdateProParte(usages));
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    exec.submit(new SolrUpdateMybatis(relations.keySet()));
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    try {
      UpdateResponse resp = solr.deleteByQuery("dataset_key:"+datasetKey.toString(), commitWithinMs);
      // TODO: extract number of deletion from response
      return 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Future<Boolean> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
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
    ExecutorUtils.stop(exec, NAME, 60, TimeUnit.SECONDS);
  }


  class SolrUpdateProParte implements Callable<Boolean> {
    private final List<NameUsage> usages;

    public SolrUpdateProParte(List<NameUsage> usages) {
      this.usages = usages;
    }

    @Override
    public Boolean call() throws Exception {
      for (NameUsage u : usages) {
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
        insertOrUpdate(u, parents, null);
      }
      return true;
    }
  }

  class SolrUpdateCallback implements Callable<Boolean> {
    private final Iterable<Integer> usages;
    private final ImporterCallback dao;

    public SolrUpdateCallback(ImporterCallback dao, Iterable<Integer> usages) {
      this.dao = dao;
      this.usages = usages;
    }

    @Override
    public Boolean call() throws Exception {
      for (Integer id : usages) {
        NameUsage u = dao.readUsage(id);
        UsageExtensions e = dao.readExtensions(id);
        insertOrUpdate(u, usageService.listParents(id), e);
      }
      return true;
    }
  }

  class SolrUpdateMybatis implements Callable<Boolean> {
    private final Collection<Integer> ids;

    public SolrUpdateMybatis(Collection<Integer> ids) {
      this.ids = ids;
    }

    @Override
    public Boolean call() throws Exception {
      for (Integer id : ids) {
        insertOrUpdate(id);
      }
      return true;
    }
  }

  class SolrDelete implements Callable<Boolean> {
    private final Collection<Integer> ids;

    public SolrDelete(List<Integer> ids) {
      this.ids = ids;
    }

    @Override
    public Boolean call() throws Exception {
      for (Integer id : ids) {
        solr.deleteById(String.valueOf(id), commitWithinMs);
      }
      return true;
    }
  }
}
