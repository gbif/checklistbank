package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageSyncService;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMetricsMapper;
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
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapperJson;
import org.gbif.checklistbank.service.mybatis.mapper.VernacularNameMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.codahale.metrics.Meter;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the insert and update methods for a single name usage and its related verbatim, extension and metrics data.
 */
public class UsageSyncServiceMyBatis implements UsageSyncService {

  private static final Logger LOG = LoggerFactory.getLogger(UsageSyncServiceMyBatis.class);

  private final UsageMapper usageMapper;
  private final NameUsageMapper nameUsageMapper;
  private final NameUsageMetricsMapper metricsMapper;
  private final NubRelMapper nubRelMapper;
  private final RawUsageMapper rawMapper;
  private final VerbatimNameUsageMapperJson vParser = new VerbatimNameUsageMapperJson();
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
  private final DatasetMetricsMapper datasetMetricsMapper;
  // global meter/counters across all datasets
  private final Meter meterUsages = new Meter();
  private final Meter meterExtensions = new Meter();
  private final AtomicInteger counterUsages = new AtomicInteger(0);
  private final AtomicInteger counterExtensions = new AtomicInteger(0);

  @Inject
  UsageSyncServiceMyBatis(UsageMapper usageMapper, NameUsageMapper nameUsageMapper,
                          NameUsageMetricsMapper metricsMapper, NubRelMapper nubRelMapper, RawUsageMapper rawMapper,
                          ParsedNameService nameService, CitationService citationService, DescriptionMapper descriptionMapper,
                          DistributionMapper distributionMapper, IdentifierMapper identifierMapper, MultimediaMapper multimediaMapper,
                          ReferenceMapper referenceMapper, SpeciesProfileMapper speciesProfileMapper, TypeSpecimenMapper typeSpecimenMapper,
                          VernacularNameMapper vernacularNameMapper, DatasetMetricsMapper datasetMetricsMapper) {
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
    this.datasetMetricsMapper = datasetMetricsMapper;
  }

  /**
   * TODO: update docs!!!
   *
   * Syncs the usage to postgres doing an update or insert depending on whether a usage with the given taxonID already existed for that dataset.
   * If the taxonID existed the previous usageKey is kept and an update of the record is done. If it did not exist yet a new usageKey is generated
   * and an insert performed.
   * All foreign keys pointing to other checklist bank entities, e.g. parentKey, must already point to existing postgres records.
   * The exception being that values of -1 are replaced with the newly generated usageKey of the newly inserted record.
   * That means in particular for the classification key (e.g. kingdomKey) that usages must be synced in taxonomic hierarchical order.
   * Root usages must be inserted first so that child usages can point to parental usages without breaking foreign key constraints in the database.
   * This DOES NOT update the solr index or anything else but postgres!
   */
  @Override
  public int syncUsage(boolean insert, NameUsage usage, ParsedName pn, NameUsageMetrics metrics) {
    Preconditions.checkNotNull(usage);
    Preconditions.checkNotNull(pn);
    Preconditions.checkNotNull(usage.getDatasetKey(), "datasetKey must exist");
    Preconditions.checkNotNull(metrics);

    // find previous usageKey based on dataset specific taxonID, the source identifier for all non backbone usages
    if (!usage.getDatasetKey().equals(Constants.NUB_DATASET_KEY)) {
      usage.setKey(nameUsageMapper.getKey(usage.getDatasetKey(), usage.getTaxonID()));
    }

    if (usage.getKey() == null || insert) {
      usage.setKey(insertNewUsage(usage, pn, metrics));
      LOG.debug("inserted usage {} with taxonID {} from dataset {}", usage.getKey(), usage.getTaxonID(), usage.getDatasetKey());
    } else {
      updateUsage(usage, pn, metrics);
      LOG.debug("updated usage {} with taxonID {} from dataset {}", usage.getKey(), usage.getTaxonID(), usage.getDatasetKey());
    }

    // count
    meterUsages.mark();
    int cnt = counterUsages.incrementAndGet();
    if (cnt % 10000 == 0) {
      LogContext.startDataset(usage.getDatasetKey());
      LOG.info("Synced {} usages, mean rate={}", cnt, meterUsages.getMeanRate());
      LogContext.endDataset();
    }
    return usage.getKey();
  }


  /**
   * TODO: update docs!!!
   */
  @Override
  public void syncUsageExtras(boolean insert, UUID datasetKey, int usageKey, @Nullable VerbatimNameUsage verbatim, @Nullable UsageExtensions extensions) {
    // insert extension data
    syncExtensions(usageKey, !insert, extensions);

    // update or insert verbatim
    // we delete and insert instead of updates to avoid updating non existing records
    // see http://dev.gbif.org/issues/browse/POR-2617
    rawMapper.delete(usageKey);

    // insert verbatim
    insertVerbatim(verbatim, datasetKey, usageKey);

    // count
    meterExtensions.mark();
    int cnt = counterExtensions.incrementAndGet();
    if (cnt % 10000 == 0) {
      LogContext.startDataset(datasetKey);
      LOG.info("Synced {} usage supplements, mean rate={}", cnt, meterExtensions.getMeanRate());
      LogContext.endDataset();
    }
  }

  @Override
  public void updateForeignKeys(int usageKey, Integer parentKey, Integer basionymKey) {
    nameUsageMapper.updateForeignKeys(usageKey, parentKey, basionymKey);
  }

  /**
   * @return the usage key for the inserted record
   */
  private int insertNewUsage(NameUsage u, ParsedName pn, NameUsageMetrics metrics) {
    final UUID datasetKey = u.getDatasetKey();

    // insert main usage, creating name and citation records before
    NameUsageWritable uw = toWritable(datasetKey, u, pn, metrics);
    nameUsageMapper.insert(uw);
    u.setKey(uw.getKey());

    // update self references indicated by -1 so that the usage does not contain any bad foreign keys anymore
    // this is needed for subsequent syncing of solr!
    updateSelfReferences(u);

    // insert usage metrics
    metrics.setKey(uw.getKey());
    metricsMapper.insert(datasetKey, metrics);

    // insert nub mapping
    if (u.getNubKey() != null && !u.getDatasetKey().equals(Constants.NUB_DATASET_KEY)) {
      nubRelMapper.insert(datasetKey, u.getKey(), u.getNubKey());
    }

    return uw.getKey();
  }

  private void insertVerbatim(@Nullable VerbatimNameUsage verbatim, UUID datasetKey, int usageKey) {
    if (verbatim != null) {
      RawUsage raw = new RawUsage();
      raw.setUsageKey(usageKey);
      raw.setDatasetKey(datasetKey);
      raw.setJson(vParser.write(verbatim));
      rawMapper.insert(raw);
    }
  }

  private void syncExtensions(final int usageKey, boolean removeBefore, UsageExtensions ext) {
    if (removeBefore) {
      // remove all previous extension records
      // remove all previous extension records
      descriptionMapper.deleteByUsage(usageKey);
      distributionMapper.deleteByUsage(usageKey);
      identifierMapper.deleteByUsage(usageKey);
      multimediaMapper.deleteByUsage(usageKey);
      referenceMapper.deleteByUsage(usageKey);
      speciesProfileMapper.deleteByUsage(usageKey);
      typeSpecimenMapper.deleteByUsage(usageKey);
      vernacularNameMapper.deleteByUsage(usageKey);
    }

    if (ext == null) return;

    try {
      for (Description d : ext.descriptions) {
        Integer sk = citationService.createOrGet(d.getSource());
        descriptionMapper.insert(usageKey, d, sk);
      }
      for (Distribution d : ext.distributions) {
        distributionMapper.insert(usageKey, d, citationService.createOrGet(d.getSource()));
      }
      for (Identifier i : ext.identifiers) {
        if (i.getType() == null) {
          i.setType(IdentifierType.UNKNOWN);
        }
        identifierMapper.insert(usageKey, i);
      }
      for (NameUsageMediaObject m : ext.media) {
        multimediaMapper.insert(usageKey, m, citationService.createOrGet(m.getSource()));
      }
      for (Reference r : ext.referenceList) {
        String citation = r.getCitation();
        if (Strings.isNullOrEmpty(citation)) {
          // try to build from pieces if full citation is not given!!!
          citation = buildCitation(r);
        }
        if (!Strings.isNullOrEmpty(citation)) {
          referenceMapper.insert(usageKey, citationService.createOrGet(citation, r.getDoi(), r.getLink()), r);
        }
      }
      for (SpeciesProfile s : ext.speciesProfiles) {
        speciesProfileMapper.insert(usageKey, s, citationService.createOrGet(s.getSource()));
      }
      for (TypeSpecimen t : ext.typeSpecimens) {
        typeSpecimenMapper.insert(usageKey, t, citationService.createOrGet(t.getSource()));
      }
      for (VernacularName v : ext.vernacularNames) {
        vernacularNameMapper.insert(usageKey, v, citationService.createOrGet(v.getSource()));
      }

    } catch (Exception e) {
      LOG.error("Failed to sync extensions for usage {}", usageKey, e);
      LOG.info("failed usage {}", ext);
      LOG.info("failed usage descriptions {}", ext.descriptions);
      LOG.info("failed usage distributions {}", ext.distributions);
      LOG.info("failed usage identifiers {}", ext.identifiers);
      LOG.info("failed usage media {}", ext.media);
      LOG.info("failed usage references {}", ext.referenceList);
      LOG.info("failed usage speciesProfiles {}", ext.speciesProfiles);
      LOG.info("failed usage typeSpecimens {}", ext.typeSpecimens);
      LOG.info("failed usage vernacularNames {}", ext.vernacularNames);
      Throwables.propagate(e);
    }
  }

  protected static String buildCitation(Reference r) {
    StringBuilder sb = new StringBuilder();
    if (!Strings.isNullOrEmpty(r.getAuthor())) {
      sb.append(r.getAuthor());
      if (Strings.isNullOrEmpty(r.getDate())) {
        sb.append(": ");
      } else {
        sb.append(" ");
      }
    }
    if (!Strings.isNullOrEmpty(r.getDate())) {
      sb.append("(");
      sb.append(r.getDate());
      sb.append(") ");
    }
    if (!Strings.isNullOrEmpty(r.getTitle())) {
      sb.append(r.getTitle());
    }
    if (!Strings.isNullOrEmpty(r.getSource())) {
      if (!Strings.isNullOrEmpty(r.getTitle())) {
        sb.append(": ");
      }
      sb.append(r.getSource());
    }
    return Strings.emptyToNull(sb.toString().trim());
  }

  /**
   * Updates an existing usage record and all its related extensions.
   * Checking whether a usage has changed is a bit of work and error prone so we always update currently.
   * In the future we should try to update only when needed though. We would need to compare the usage itself,
   * the raw data, the usage metrics and all extension data in the container though.
   *
   * @param u updated usage
   */
  private void updateUsage(NameUsage u, ParsedName pn, NameUsageMetrics metrics) {
    final UUID datasetKey = u.getDatasetKey();

    // update self references indicated by -1
    updateSelfReferences(u);
    // insert main usage, creating name and citation records before
    NameUsageWritable uw = toWritable(datasetKey, u, pn, metrics);
    nameUsageMapper.update(uw);

    // update usage metrics
    metrics.setKey(u.getKey());
    metricsMapper.update(metrics);

    // update nub mapping for non backbone records
    if (!Constants.NUB_DATASET_KEY.equals(u.getDatasetKey())) {
      nubRelMapper.delete(u.getKey());
      if (u.getNubKey() != null) {
        nubRelMapper.insert(datasetKey, u.getKey(), u.getNubKey());
      }
    }
  }

  private void updateSelfReferences(NameUsage u) {
    if (u.getBasionymKey() != null && u.getBasionymKey() == -1) {
      u.setBasionymKey(u.getKey());
    }
    if (u.getParentKey() != null && u.getParentKey() == -1) {
      u.setParentKey(u.getKey());
    }
    if (u.getAcceptedKey() != null && u.getAcceptedKey() == -1) {
      u.setAcceptedKey(u.getKey());
    }
    if (u.getProParteKey() != null && u.getProParteKey() == -1) {
      u.setProParteKey(u.getKey());
    }
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (u.getHigherRankKey(r) != null && u.getHigherRankKey(r) == -1) {
        ClassificationUtils.setHigherRankKey(u, r, u.getKey());
      }
    }
  }

  /**
   * Converts a name usage into a writable name usage by looking up or inserting name and citation records
   * and populating the writable instance with these keys.
   */
  private NameUsageWritable toWritable(UUID datasetKey, NameUsage u, ParsedName pn, NameUsageMetrics metrics) {
    NameUsageWritable uw = new NameUsageWritable();

    uw.setKey(u.getKey());
    uw.setTaxonID(u.getTaxonID());
    uw.setSourceTaxonKey(u.getSourceTaxonKey());
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
    uw.setNumDescendants(metrics.getNumDescendants());
    uw.setNomenclaturalStatus(u.getNomenclaturalStatus());
    uw.setTaxonomicStatus(u.getTaxonomicStatus());
    uw.setReferences(u.getReferences());
    uw.setRemarks(u.getRemarks());
    uw.setModified(u.getModified());
    uw.setIssues(u.getIssues());

    // lookup or insert name record
    pn = nameService.createOrGet(pn);
    uw.setNameKey(pn.getKey());

    // lookup or insert citation records
    uw.setPublishedInKey(citationService.createOrGet(u.getPublishedIn()));
    uw.setAccordingToKey(citationService.createOrGet(u.getAccordingTo()));

    return uw;
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    nubRelMapper.deleteByDataset(datasetKey);
    for (List<Integer> batch : Iterables.partition(relations.keySet(), 10000)) {
      insertNubRelationBatch(datasetKey, relations, batch);
    }
  }

  @Transactional(
      executorType = ExecutorType.BATCH,
      isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
      exceptionMessage = "Something went wrong while inserting nub relations batch for dataset {0}"
  )
  private void insertNubRelationBatch(UUID datasetKey, Map<Integer, Integer> relations, Iterable<Integer> usageKeyBatch) {
    for (Integer usageKey : usageKeyBatch) {
      Set<NameUsageIssue> issues = nameUsageMapper.getIssues(usageKey).getIssues();
      if (relations.get(usageKey) == null) {
        // no match, add issue if not existing yet
        if (!issues.contains(NameUsageIssue.BACKBONE_MATCH_NONE)) {
          issues.add(NameUsageIssue.BACKBONE_MATCH_NONE);
          nameUsageMapper.updateIssues(usageKey, issues);
        }

      } else {
        if (issues.remove(NameUsageIssue.BACKBONE_MATCH_NONE) || issues.remove(NameUsageIssue.BACKBONE_MATCH_FUZZY)) {
          nameUsageMapper.updateIssues(usageKey, issues);
        }
        nubRelMapper.insert(datasetKey, usageKey, relations.get(usageKey));
      }
    }
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    if (Constants.NUB_DATASET_KEY.equals(datasetKey)) {
      throw new IllegalArgumentException("The GBIF backbone cannot be deleted!");
    }
    LogContext.startDataset(datasetKey);
    LOG.info("Deleting entire dataset {}", datasetKey);
    LogContext.endDataset();
    int numDeleted = usageMapper.deleteByDataset(datasetKey);
    // we do not remove old dataset metrics, just add a new, empty one as the most recent
    datasetMetricsMapper.insert(datasetKey, new Date());
    return numDeleted;
  }

  @Override
  public void delete(int key) {
    if (key > Constants.NUB_MAXIMUM_KEY) {
      usageMapper.delete(key);
    } else {
      // we only logically delete nub usages
      usageMapper.deleteLogically(key);
    }
  }

}
