package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.model.Usage;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMetricsMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NubRelMapper;
import org.gbif.checklistbank.service.mybatis.mapper.RawUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageMapper;
import org.gbif.checklistbank.utils.VerbatimNameUsageMapper;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the NameUsageService using MyBatis.
 * All PagingResponses will not have the count set as it can be too costly sometimes.
 * Write operations DO NOT update the solr index or anything else than postgres!
 */
public class DatasetImportServiceMyBatis implements DatasetImportService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetImportServiceMyBatis.class);

  private final UsageMapper usageMapper;
  private final NameUsageMapper nameUsageMapper;
  private final NameUsageMetricsMapper metricsMapper;
  private final NubRelMapper nubRelMapper;
  private final RawUsageMapper rawMapper;
  private final VerbatimNameUsageMapper vParser = new VerbatimNameUsageMapper();
  private final ParsedNameService nameService;
  private final CitationService citationService;

  @Inject
  private DataSource ds;

  @Inject
  DatasetImportServiceMyBatis(
    UsageMapper usageMapper,
    NameUsageMapper nameUsageMapper,
    NameUsageMetricsMapper metricsMapper,
    NubRelMapper nubRelMapper,
    RawUsageMapper rawMapper,
    ParsedNameService nameService,
    CitationService citationService
  ) {
    this.nameUsageMapper = nameUsageMapper;
    this.metricsMapper = metricsMapper;
    this.nameService = nameService;
    this.usageMapper = usageMapper;
    this.nubRelMapper = nubRelMapper;
    this.citationService = citationService;
    this.rawMapper = rawMapper;
  }

  @Override
  /**
   * This DOES NOT update the solr index or anything else but postgres!
   */
  public void insertUsages(UUID datasetKey, Iterator<Usage> iter) {
    final int BATCH_SIZE = 1000;

    int batchCounter = 1;
    List<Usage> batch = Lists.newArrayList();
    while (iter.hasNext()) {
      batch.add(iter.next());
      if (batch.size() % BATCH_SIZE == 0) {
        LOG.debug("Insert nub usage batch {}", batchCounter);
        insertUsageBatch(datasetKey, batch);
        batchCounter++;
        batch.clear();
      }
    }
    LOG.debug("Insert last nub usage batch {}", batchCounter);
    insertUsageBatch(datasetKey, batch);
  }

  @Override
  /**
   * This DOES NOT update the solr index or anything else but postgres!
   */
  public int syncUsage(final UUID datasetKey, NameUsageContainer usage, @Nullable VerbatimNameUsage verbatim,
                           NameUsageMetrics metrics) {
    Preconditions.checkNotNull(datasetKey);
    Preconditions.checkNotNull(usage);
    Preconditions.checkNotNull(metrics);
    List<NameUsage> resp = nameUsageMapper.listByTaxonId(datasetKey, usage.getTaxonID(), new PagingRequest());

    int key;
    if (resp.isEmpty()) {
      key = insertNewUsage(datasetKey, usage, verbatim, metrics);
      LOG.debug("inserted usage {} with taxonID {} from dataset {}", key, usage.getTaxonID(), datasetKey);
    } else {
      if (resp.size() > 1) {
        LOG.warn("taxonID {} from dataset {} exists {} times", usage.getTaxonID(), datasetKey, resp.size());
      }
      key = updateUsage(datasetKey, resp.get(0), usage, verbatim, metrics);
      LOG.debug("updated usage {} with taxonID {} from dataset {}", key, usage.getTaxonID(), datasetKey);
    }
    return key;
  }

  private int insertNewUsage(UUID datasetKey, NameUsageContainer usage, @Nullable VerbatimNameUsage verbatim,
                                 NameUsageMetrics metrics) {

    // insert main usage, creating name and citation records before
    NameUsageWritable uw = toWritable(datasetKey, usage);
    nameUsageMapper.insert(uw);
    final int usageKey = uw.getKey();
      //TODO: deal with extensions

    // insert usage metrics
    metrics.setKey(usageKey);
    metricsMapper.insert(datasetKey, metrics);

    // insert verbatim
    if (verbatim != null) {
      RawUsage raw = new RawUsage();
      raw.setUsageKey(usageKey);
      raw.setDatasetKey(datasetKey);
      raw.setData(vParser.write(verbatim));
      rawMapper.insert(raw);
    }

    return usageKey;
  }

  /**
   * Updates an existing usage record and all its related extensions.
   * Checking whether a usage has changed is a bit of work and error prone so we always update currently.
   * In the future we should try to update only when needed though. We would need to compare the usage itself,
   * the raw data, the usage metrics and all extension data in the container though.
   * @param datasetKey
   * @param nameUsage
   * @param usage
   * @param verbatim
   * @param metrics
   * @return
   */
  private Integer updateUsage(UUID datasetKey, NameUsage nameUsage, NameUsageContainer usage,
                              @Nullable VerbatimNameUsage verbatim, NameUsageMetrics metrics) {
    final int usageKey = nameUsage.getKey();
    // insert main usage, creating name and citation records before
    NameUsageWritable uw = toWritable(datasetKey, usage);
    uw.setKey(usageKey);
    nameUsageMapper.update(uw);
    //TODO: deal with extensions

    // update usage metrics
    metrics.setKey(usageKey);
    metricsMapper.update(metrics);

    // update verbatim
    if (verbatim != null) {
      RawUsage raw = new RawUsage();
      raw.setUsageKey(usageKey);
      raw.setDatasetKey(datasetKey);
      raw.setData(vParser.write(verbatim));
      rawMapper.update(raw);
    }

    return usageKey;
  }

  /**
   * Converts a name usage into a writable name usage by looking up or inserting name and citation records
   * and populating the writable instance with these keys.
   * @param u
   * @return
   */
  private NameUsageWritable toWritable(UUID datasetKey, NameUsageContainer u) {
    NameUsageWritable uw = new NameUsageWritable();

    uw.setTaxonID(u.getTaxonID());
    uw.setDatasetKey(datasetKey);
    uw.setConstituentKey(u.getConstituentKey());

    uw.setBasionymKey(u.getBasionymKey());
    if (u.getAcceptedKey() != null) {
      uw.setParentKey(u.getAcceptedKey());
    } else {
      uw.setParentKey(u.getParentKey());
    }
    ClassificationUtils.copyLinneanClassificationKeys(u, uw);
    //TODO: duplicate records when we have multiple accepted ???
    uw.setProParteKey(null);

    uw.setRank(u.getRank());
    uw.setOrigin(u.getOrigin());
    uw.setSynonym(u.isSynonym());
    uw.setNumDescendants(u.getNumDescendants());
    uw.setNomenclaturalStatus(u.getNomenclaturalStatus());
    uw.setTaxonomicStatus(u.getTaxonomicStatus());
    uw.setReferences(u.getReferences());
    uw.setRemarks(u.getRemarks());
    uw.setModified(u.getModified());

    // lookup or create name record
    ParsedName pn = nameService.createOrGet(u.getScientificName());
    uw.setNameKey(pn.getKey());

    // lookup or create citation records
    if (!Strings.isNullOrEmpty(u.getPublishedIn())) {
      uw.setPublishedInKey( citationService.createOrGet(u.getPublishedIn()) );
    }
    if (!Strings.isNullOrEmpty(u.getAccordingTo())) {
      uw.setAccordingToKey( citationService.createOrGet(u.getAccordingTo()) );
    }

    return uw;
  }

  @Transactional(
    executorType = ExecutorType.BATCH,
    isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
    exceptionMessage = "Something went wrong while updating basionym keys for dataset {0}"
  )
  @Override
  public void updateBasionyms(UUID datasetKey, Map<Integer, Integer> basionymByUsage) {
    throw new UnsupportedOperationException();
  }

  @Transactional(
    executorType = ExecutorType.BATCH,
    isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
    exceptionMessage = "Something went wrong while inserting nub relations batch for dataset {0}"
  )
  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    nubRelMapper.deleteByDataset(datasetKey);
    for (Map.Entry<Integer, Integer> entry : relations.entrySet()) {
      nubRelMapper.insert(datasetKey, entry.getKey(), entry.getValue());
    }
  }

  @Transactional(
    executorType = ExecutorType.BATCH,
    isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
    exceptionMessage = "Something went wrong while inserting usage batch for dataset {0}"
  )
  private void insertUsageBatch(UUID datasetKey, List<Usage> usages) {
    for (Usage u : usages) {
      usageMapper.insert(datasetKey, u);
    }
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    LOG.info("Deleting entire dataset {}", datasetKey);
    return usageMapper.deleteByDataset(datasetKey);
  }

  @Override
  public int deleteOldUsages(UUID datasetKey, Date before) {
    LOG.info("Deleting all usages in dataset {} before {}", datasetKey, before);
    return usageMapper.deleteByDatasetAndDate(datasetKey, before);
  }

  @Override
  public List<Integer> listOldUsages(UUID datasetKey, Date before) {
    return usageMapper.listByDatasetAndDate(datasetKey, before);
  }

}
