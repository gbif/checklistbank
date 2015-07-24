package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.MachineTagUtils;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.nub.model.NubTags;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UsageSource reading usage data from checklistbank .
 * The list of source datasets is discovered by looking for the registry machine tag nub.gbif.org:priority
 */
public class ClbUsageSource implements UsageSource {

    private static final Logger LOG = LoggerFactory.getLogger(ClbUsageSource.class);
    private List<NubSource> sources;
    private final DatasetService datasetService;
    private final OrganizationService organizationService;
    private final ClbConfiguration clbCfg;

    public ClbUsageSource(NubConfiguration cfg) {
        this.clbCfg = cfg.clb;
        LOG.info("Loading backbone sources from registry {}", cfg.registry.wsUrl);
        Injector regInj = cfg.registry.createRegistryInjector();
        datasetService = regInj.getInstance(DatasetService.class);
        organizationService = regInj.getInstance(OrganizationService.class);
    }

    public ClbUsageSource(DatasetService datasetService, OrganizationService organizationService, ClbConfiguration cfg) {
        this.datasetService = datasetService;
        this.organizationService = organizationService;
        clbCfg = cfg;
    }

    @Override
    public List<NubSource> listSources() {
        if (sources == null) {
            loadSourcesFromRegistry();
        }
        return sources;
    }

    private <T extends MachineTaggable & NetworkEntity> Integer getNubPriority(T entity) {
        MachineTag priority = MachineTagUtils.firstTag(entity, NubTags.NAMESPACE, NubTags.PRIORITY.tag);
        if (priority != null) {
            try {
                return Integer.valueOf(priority.getValue());
            } catch (NumberFormatException e) {
                LOG.warn("Bad backbone priority for entity {} is not an integer: {}. Ignore", entity.getKey(), priority.getValue());
            }
        }
        return null;
    }

    private <T extends MachineTaggable & NetworkEntity> Rank getNubRank(T entity) {
        MachineTag rank = MachineTagUtils.firstTag(entity, NubTags.NAMESPACE, NubTags.RANK_LIMIT.tag);
        if (rank != null) {
            try {
                return VocabularyUtils.lookupEnum(rank.getValue(), Rank.class);
            } catch (IllegalArgumentException e) {
                LOG.warn("Bad backbone rank tag for entity {}: {}. Ignore", entity.getKey(), rank.getValue());
            }
        }
        return null;
    }

    private NubSource buildSource(Dataset d, int priority, Rank rank) {
        NubSource src = new NubSource();
        src.key = d.getKey();
        src.name = d.getTitle();
        src.created = d.getCreated();
        src.priority = priority;
        if (rank != null) {
            src.ignoreRanksAbove = rank;
        }
        return src;
    }

    private void loadSourcesFromRegistry() {
        sources = Lists.newArrayList();
        Set<UUID> keys = Sets.newHashSet();
        for (Dataset d : Iterables.datasets(DatasetType.CHECKLIST, datasetService)) {
            Integer priority = getNubPriority(d);
            if (priority != null) {
                Rank rank = getNubRank(d);
                NubSource src = buildSource(d, priority, rank);
                keys.add(d.getKey());
                sources.add(src);
            }
        }
        // look for tagged organizations, e.g. Pensoft journals
        for (Organization org : Iterables.organizations(null, organizationService)) {
            Integer priority = getNubPriority(org);
            Rank rank = getNubRank(org);
            if (priority != null) {
                int counter = 0;
                for (Dataset d : Iterables.publishedDatasets(org.getKey(), DatasetType.CHECKLIST, organizationService)) {
                    if (!keys.contains(d.getKey())) {
                        NubSource src = buildSource(d, priority, rank);
                        keys.add(d.getKey());
                        sources.add(src);
                        counter++;
                    }
                }
                LOG.info("Found {} new nub sources published by organization {} {}", counter, org.getKey(), org.getTitle());
            }
        }
        LOG.info("Found {} tagged backbone sources in the registry", sources.size());

        // sort source according to priority
        Ordering<NubSource> order = Ordering
            .natural()
            //.reverse()
            .onResultOf(new Function<NubSource, Integer>() {
                @Nullable
                @Override
                public Integer apply(NubSource input) {
                    return input.priority;
                }
            })
            .compound(Ordering
                            .natural()
                            .reverse()  // newest first, e.g. pensoft articles
                            .nullsLast()
                            .onResultOf(new Function<NubSource, Date>() {
                                @Nullable
                                @Override
                                public Date apply(NubSource input) {
                                    return input.created;
                                }
                            })
            );
        sources = order.sortedCopy(sources);
    }

    @Override
    public Iterable<SrcUsage> iterateSource(NubSource source) {
        try {
            return new ClbUsageIteratorNeo(clbCfg, source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
