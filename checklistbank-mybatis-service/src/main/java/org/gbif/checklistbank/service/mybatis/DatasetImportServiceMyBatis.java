package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.model.Usage;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.mapper.DescriptionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.IdentifierMapper;
import org.gbif.checklistbank.service.mybatis.mapper.MultimediaMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMetricsMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NubRelMapper;
import org.gbif.checklistbank.service.mybatis.mapper.RawUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ReferenceMapper;
import org.gbif.checklistbank.service.mybatis.mapper.SpeciesProfileMapper;
import org.gbif.checklistbank.service.mybatis.mapper.TypeSpecimenMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.VernacularNameMapper;
import org.gbif.checklistbank.utils.VerbatimNameUsageMapper;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
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
  private final DescriptionMapper descriptionMapper;
  private final DistributionMapper distributionMapper;
  private final IdentifierMapper identifierMapper;
  private final MultimediaMapper multimediaMapper;
  private final ReferenceMapper referenceMapper;
  private final SpeciesProfileMapper speciesProfileMapper;
  private final TypeSpecimenMapper typeSpecimenMapper;
  private final VernacularNameMapper vernacularNameMapper;

  @Inject
  DatasetImportServiceMyBatis(UsageMapper usageMapper, NameUsageMapper nameUsageMapper,
    NameUsageMetricsMapper metricsMapper, NubRelMapper nubRelMapper, RawUsageMapper rawMapper,
    ParsedNameService nameService, CitationService citationService, DescriptionMapper descriptionMapper,
    DistributionMapper distributionMapper, IdentifierMapper identifierMapper, MultimediaMapper multimediaMapper,
    ReferenceMapper referenceMapper, SpeciesProfileMapper speciesProfileMapper, TypeSpecimenMapper typeSpecimenMapper,
    VernacularNameMapper vernacularNameMapper) {
    this.nameUsageMapper = nameUsageMapper;
    this.metricsMapper = metricsMapper;
    this.nameService = nameService;
    this.usageMapper = usageMapper;
    this.nubRelMapper = nubRelMapper;
    this.citationService = citationService;
    this.rawMapper = rawMapper;
    this.descriptionMapper = descriptionMapper;
    this.distributionMapper = distributionMapper;
    this.identifierMapper = identifierMapper;
    this.multimediaMapper = multimediaMapper;
    this.referenceMapper = referenceMapper;
    this.speciesProfileMapper = speciesProfileMapper;
    this.typeSpecimenMapper = typeSpecimenMapper;
    this.vernacularNameMapper = vernacularNameMapper;
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
    usage.setKey(usageKey);

    // insert extension data
    insertExtensions(usage);

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

  private void insertExtensions(NameUsageContainer usage) {
    for (Description d : usage.getDescriptions()) {
      Integer sk = citationService.createOrGet(d.getSource());
      descriptionMapper.insert(usage.getKey(), d, sk);
    }
    for (Distribution d : usage.getDistributions()) {
      distributionMapper.insert(usage.getKey(), d, citationService.createOrGet(d.getSource()));
    }
    for (Identifier i : usage.getIdentifiers()) {
      if (i.getType() == null) {
        i.setType(IdentifierType.UNKNOWN);
      }
      identifierMapper.insert(usage.getKey(), i);
    }
    for (NameUsageMediaObject m : usage.getMedia()) {
      multimediaMapper.insert(usage.getKey(), m, citationService.createOrGet(m.getSource()));
    }
    for (Reference r : usage.getReferenceList()) {
      referenceMapper.insert(usage.getKey(), citationService.createOrGet(r.getCitation()), r, citationService.createOrGet(r.getSource()));
    }
    for (SpeciesProfile s : usage.getSpeciesProfiles()) {
      speciesProfileMapper.insert(usage.getKey(), s, citationService.createOrGet(s.getSource()));
    }
    for (TypeSpecimen t : usage.getTypeSpecimens()) {
      typeSpecimenMapper.insert(usage.getKey(), t, citationService.createOrGet(t.getSource()));
    }
    for (VernacularName v : usage.getVernacularNames()) {
      vernacularNameMapper.insert(usage.getKey(), v, citationService.createOrGet(v.getSource()));
    }
  }

  /**
   * Updates an existing usage record and all its related extensions.
   * Checking whether a usage has changed is a bit of work and error prone so we always update currently.
   * In the future we should try to update only when needed though. We would need to compare the usage itself,
   * the raw data, the usage metrics and all extension data in the container though.
   * @param datasetKey
   * @param existing existing name usage
   * @param usage updated usage
   * @param verbatim
   * @param metrics
   * @return
   */
  private Integer updateUsage(UUID datasetKey, NameUsage existing, NameUsageContainer usage,
                              @Nullable VerbatimNameUsage verbatim, NameUsageMetrics metrics) {
    final int usageKey = existing.getKey();
    usage.setKey(usageKey);
    // update self references indicated by -1
    updateSelfReferences(usageKey, usage);
    // insert main usage, creating name and citation records before
    NameUsageWritable uw = toWritable(datasetKey, usage);
    uw.setKey(usageKey);
    nameUsageMapper.update(uw);

    // remove all previous extension records
    descriptionMapper.deleteByUsage(usageKey);
    distributionMapper.deleteByUsage(usageKey);
    identifierMapper.deleteByUsage(usageKey);
    multimediaMapper.deleteByUsage(usageKey);
    referenceMapper.deleteByUsage(usageKey);
    speciesProfileMapper.deleteByUsage(usageKey);
    typeSpecimenMapper.deleteByUsage(usageKey);
    vernacularNameMapper.deleteByUsage(usageKey);
    // insert new extension data
    insertExtensions(usage);

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

  private void updateSelfReferences(int usageKey, NameUsage u) {
    if (u.getBasionymKey() != null && u.getBasionymKey() == -1) {
      u.setBasionymKey(usageKey);
    }
    if (u.getParentKey() != null && u.getParentKey() == -1) {
      u.setParentKey(usageKey);
    }
    if (u.getAcceptedKey() != null && u.getAcceptedKey() == -1) {
      u.setAcceptedKey(usageKey);
    }
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (u.getHigherRankKey(r) != null && u.getHigherRankKey(r) == -1) {
        ClassificationUtils.setHigherRankKey(u, r, usageKey);
      }
    }
  }

  /**
   * Converts a name usage into a writable name usage by looking up or inserting name and citation records
   * and populating the writable instance with these keys.
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
    uw.setProParteKey(u.getProParteKey());

    uw.setRank(u.getRank());
    uw.setOrigin(u.getOrigin());
    uw.setSynonym(u.isSynonym());
    uw.setNumDescendants(u.getNumDescendants());
    uw.setNomenclaturalStatus(u.getNomenclaturalStatus());
    uw.setTaxonomicStatus(u.getTaxonomicStatus());
    uw.setReferences(u.getReferences());
    uw.setRemarks(u.getRemarks());
    uw.setModified(u.getModified());
    uw.setIssues(u.getIssues());

    // lookup or insert name record
    ParsedName pn = nameService.createOrGet(u.getScientificName());
    uw.setNameKey(pn.getKey());

    // lookup or insert citation records
    uw.setPublishedInKey( citationService.createOrGet(u.getPublishedIn()) );
    uw.setAccordingToKey( citationService.createOrGet(u.getAccordingTo()) );

    return uw;
  }

  @Transactional(
    executorType = ExecutorType.BATCH,
    isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
    exceptionMessage = "Something went wrong while updating basionym keys for dataset {0}"
  )
  @Override
  public void updateBasionyms(UUID datasetKey, Map<Integer, Integer> basionymByUsage) {
    for (Map.Entry<Integer, Integer> entry : basionymByUsage.entrySet()) {
      nameUsageMapper.updateBasionymKey(entry.getKey(), entry.getValue());
    }
  }

  @Transactional(
    executorType = ExecutorType.BATCH,
    isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
    exceptionMessage = "Something went wrong while updating proparte keys for dataset {0}"
  )
  @Override
  public void updateProparte(UUID datasetKey, Map<Integer, Integer> proparteByUsage) {
    for (Map.Entry<Integer, Integer> entry : proparteByUsage.entrySet()) {
      nameUsageMapper.updateProparteKey(entry.getKey(), entry.getValue());
    }
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
