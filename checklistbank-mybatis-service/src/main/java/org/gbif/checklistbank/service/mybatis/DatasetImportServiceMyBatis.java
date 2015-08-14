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
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.concurrent.NamedThreadFactory;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ParsedNameService;
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
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.VerbatimNameUsageMapperKryo;
import org.gbif.checklistbank.service.mybatis.mapper.VernacularNameMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
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
public class DatasetImportServiceMyBatis implements DatasetImportService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetImportServiceMyBatis.class);

    private final UsageMapper usageMapper;
    private final NameUsageMapper nameUsageMapper;
    private final NameUsageMetricsMapper metricsMapper;
    private final NubRelMapper nubRelMapper;
    private final RawUsageMapper rawMapper;
    private final VerbatimNameUsageMapper vParser = new VerbatimNameUsageMapperKryo();
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
    private ExecutorService exec;

    @Inject
    DatasetImportServiceMyBatis(UsageMapper usageMapper, NameUsageMapper nameUsageMapper,
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
     * Assigns the service a thread pool to use when syncing rich usage records.
     * The core NameUsage is always handled by the main thread, but extension data sql operations can be executed by this thread pool in parallel
     * @param poolSize
     */
    public void useParallelSyncing(int poolSize) {
        if (exec != null) {
            // we already had a thread pool. Warn and shutdown existing one
            LOG.warn("Setting new thread pool with size {}, but there was an existing thread pool which is being shutdown", poolSize);
            exec.shutdown();
        }
        if (poolSize > 0) {
            LOG.info("Use {} threads to run import sync statements in parallel", poolSize);
            exec = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory("import-mybatis"));
        }
    }

    @Override
    /**
     * Syncs the usage to postgres doing an update or insert depending on whether a usage with the given taxonID already existed for that dataset.
     * If the taxonID existed the previous usageKey is kept and an update of the record is done. If it did not exist yet a new usageKey is generated
     * and an insert performed.
     *
     * All foreign keys pointing to other checklist bank entities, e.g. parentKey, must already point to existing postgres records.
     * The exception being that values of -1 are replaced with the newly generated usageKey of the newly inserted record.
     * That means in particular for the classification key (e.g. kingdomKey) that usages must be synced in taxonomic hierarchical order.
     * Root usages must be inserted first so that child usages can point to parental usages without breaking foreign key constraints in the database.
     *
     * This DOES NOT update the solr index or anything else but postgres!
     */
    public int syncUsage(boolean insert, NameUsage usage, @Nullable VerbatimNameUsage verbatim, NameUsageMetrics metrics, @Nullable UsageExtensions extensions) {
        Preconditions.checkNotNull(usage);
        Preconditions.checkNotNull(usage.getDatasetKey(), "datasetKey must exist");
        Preconditions.checkNotNull(metrics);

        // find previous usageKey based on dataset specific taxonID, the source identifier for all non backbone usages
        if (usage.getDatasetKey() != Constants.NUB_DATASET_KEY) {
            usage.setKey(nameUsageMapper.getKey(usage.getDatasetKey(), usage.getTaxonID()));
        }

        if (usage.getKey() == null || insert) {
            usage.setKey( insertNewUsage(usage, verbatim, metrics, extensions) );
            LOG.debug("inserted usage {} with taxonID {} from dataset {}", usage.getKey(), usage.getTaxonID(), usage.getDatasetKey());
        } else {
            updateUsage(usage, verbatim, metrics, extensions);
            LOG.debug("updated usage {} with taxonID {} from dataset {}", usage.getKey(), usage.getTaxonID(), usage.getDatasetKey());
        }
        return usage.getKey();
    }

    @Override
    public void updateForeignKeys(int usageKey, Integer parentKey, Integer basionymKey) {
        nameUsageMapper.updateForeignKeys(usageKey, parentKey, basionymKey);
    }

    private int insertNewUsage(NameUsage u, @Nullable VerbatimNameUsage verbatim, NameUsageMetrics metrics, @Nullable UsageExtensions extensions) {
        final UUID datasetKey = u.getDatasetKey();

        // insert main usage, creating name and citation records before
        NameUsageWritable uw = toWritable(datasetKey, u, metrics);
        nameUsageMapper.insert(uw);
        u.setKey(uw.getKey());

        // update self references indicated by -1 so that the usage does not contain any bad foreign keys anymore
        // this is needed for subsequent syncing of solr!
        updateSelfReferences(u);

        // insert extension data
        syncExtensions(u.getKey(), false, extensions);

        // insert usage metrics
        metrics.setKey(u.getKey());
        metricsMapper.insert(datasetKey, metrics);

        // insert verbatim
        insertVerbatim(verbatim, datasetKey, u.getKey());

        // insert nub mapping
        if (u.getNubKey() != null && u.getDatasetKey() != Constants.NUB_DATASET_KEY) {
            nubRelMapper.insert(datasetKey, u.getKey(), u.getNubKey());
        }

        return uw.getKey();
    }

    private void insertVerbatim(@Nullable VerbatimNameUsage verbatim, UUID datasetKey, int usageKey) {
        if (verbatim != null) {
            RawUsage raw = new RawUsage();
            raw.setUsageKey(usageKey);
            raw.setDatasetKey(datasetKey);
            raw.setData(vParser.write(verbatim));
            rawMapper.insert(raw);
        }
    }

    private void syncExtensions(final int usageKey, boolean removeBefore, UsageExtensions extensions) {
        if (exec == null) {
            // no threads to sync extension data
            syncExtensionsInternal(usageKey, removeBefore, extensions);
        } else {
            exec.submit(new SyncExtension(usageKey, removeBefore, extensions));
        }
    }

    private void syncExtensionsInternal(final int usageKey, boolean removeBefore, UsageExtensions ext) {
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
                    referenceMapper.insert(usageKey, citationService.createOrGet(citation), r, citationService.createOrGet(r.getSource()));
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
     * @param u    updated usage
     */
    private void updateUsage(NameUsage u, @Nullable VerbatimNameUsage verbatim, NameUsageMetrics metrics, UsageExtensions extensions) {
        final UUID datasetKey = u.getDatasetKey();

        // update self references indicated by -1
        updateSelfReferences(u);
        // insert main usage, creating name and citation records before
        NameUsageWritable uw = toWritable(datasetKey, u, metrics);
        nameUsageMapper.update(uw);

        // sync extension data
        syncExtensions(u.getKey(), true, extensions);

        // update usage metrics
        metrics.setKey(u.getKey());
        metricsMapper.update(metrics);

        // update verbatim
        // we delete and insert instead of updates to avoid updating non existing records
        // see http://dev.gbif.org/issues/browse/POR-2617
        rawMapper.delete(u.getKey());
        insertVerbatim(verbatim, datasetKey, u.getKey());

        // update nub mapping
        nubRelMapper.delete(u.getKey());
        if (u.getNubKey() != null) {
            nubRelMapper.insert(datasetKey, u.getKey(), u.getNubKey());
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
    private NameUsageWritable toWritable(UUID datasetKey, NameUsage u, NameUsageMetrics metrics) {
        NameUsageWritable uw = new NameUsageWritable();

        uw.setKey(u.getKey());
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
        uw.setNumDescendants(metrics.getNumDescendants());
        uw.setNomenclaturalStatus(u.getNomenclaturalStatus());
        uw.setTaxonomicStatus(u.getTaxonomicStatus());
        uw.setReferences(u.getReferences());
        uw.setRemarks(u.getRemarks());
        uw.setModified(u.getModified());
        uw.setIssues(u.getIssues());

        // lookup or insert name record
        ParsedName pn = nameService.createOrGet(u.getScientificName(), u.getRank());
        uw.setNameKey(pn.getKey());

        // lookup or insert citation records
        uw.setPublishedInKey(citationService.createOrGet(u.getPublishedIn()));
        uw.setAccordingToKey(citationService.createOrGet(u.getAccordingTo()));

        return uw;
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

    @Override
    public int deleteDataset(UUID datasetKey) {
        if (Constants.NUB_DATASET_KEY.equals(datasetKey)) {
            throw new IllegalArgumentException("The GBIF backbone cannot be deleted!");
        }
        LOG.info("Deleting entire dataset {}", datasetKey);
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

    @Override
    public List<Integer> listOldUsages(UUID datasetKey, Date before) {
        return usageMapper.listByDatasetAndDate(datasetKey, before);
    }

    @Override
    public void close() throws Exception {
        if (exec != null) {
            exec.shutdown();
        }
    }

    class SyncExtension implements Runnable {
        private final int usageKey;
        private final boolean delete;
        private final UsageExtensions extensions;

        public SyncExtension(int usageKey, boolean delete, UsageExtensions extensions) {
            this.usageKey = usageKey;
            this.delete = delete;
            this.extensions = extensions;
        }

        @Override
        public void run() {
            syncExtensionsInternal(usageKey, delete, extensions);
        }
    }
}
