package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.io.CSVReader;
import org.gbif.utils.file.FileUtils;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.neo4j.helpers.Strings;
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
    private final NubConfiguration cfg;

    public ClbUsageSource(NubConfiguration cfg) {
        this.cfg = cfg;
        LOG.info("Loading backbone sources from registry {}", cfg.registry.wsUrl);
        Injector regInj = cfg.registry.createRegistryInjector();
        datasetService = regInj.getInstance(DatasetService.class);
        organizationService = regInj.getInstance(OrganizationService.class);
    }

    public ClbUsageSource(DatasetService datasetService, OrganizationService organizationService, NubConfiguration cfg) {
        this.datasetService = datasetService;
        this.organizationService = organizationService;
        this.cfg = cfg;
    }

    @Override
    public List<NubSource> listSources() {
        if (sources == null) {
            loadSources();
        }
        return sources;
    }

    private NubSource buildSource(Dataset d, int priority, Rank rank) {
        NubSource src = new NubSource();
        src.key = d.getKey();
        src.name = d.getTitle();
        src.created = d.getCreated();
        src.priority = priority;
        src.nomenclator = DatasetSubtype.NOMENCLATOR_AUTHORITY == d.getSubtype();
        if (rank != null) {
            src.ignoreRanksAbove = rank;
        }
        return src;
    }

    private void loadSources() {
        Set<UUID> keys = Sets.newHashSet();
        sources = Lists.newArrayList();

        try {
            InputStream stream;
            if (cfg.sourceList.isAbsolute()) {
                stream = cfg.sourceList.toURL().openStream();
            } else {
                stream = FileUtils.classpathStream(cfg.sourceList.toString());
            }
            CSVReader reader = new CSVReader(stream, "UTF-8", "\t", null, 0);
            Integer priority = 0;
            for (String[] row : reader) {
                if (row.length < 1) continue;
                UUID key = UUID.fromString(row[0]);
                if (keys.contains(key)) continue;
                keys.add(key);
                priority++;

                Rank rank = row.length > 1 && !Strings.isBlank(row[1]) ? Rank.valueOf(row[1]) : null;
                Dataset d = datasetService.get(key);
                if (d != null) {
                    NubSource src = buildSource(d, priority, rank);
                    sources.add(src);

                } else {
                    // try if its an organization
                    Organization org = organizationService.get(key);
                    if (org == null) {
                        LOG.warn("Unknown nub source {}. Ignore", key);
                    } else {
                        int counter = 0;
                        for (Dataset d2 : Iterables.publishedDatasets(org.getKey(), DatasetType.CHECKLIST, organizationService)) {
                            if (!keys.contains(d2.getKey())) {
                                NubSource src = buildSource(d2, priority, rank);
                                sources.add(src);
                                counter++;
                            }
                        }
                        LOG.info("Found {} new nub sources published by organization {} {}", counter, org.getKey(), org.getTitle());
                    }
                }
            }

        } catch (Exception e) {
            LOG.error("Cannot read nub sources from {}", cfg.sourceList);
            throw new RuntimeException(e);
        }
        LOG.info("Found {} backbone sources", sources.size());

        // sort source according to priority and date created (for org datasets!)
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
    public SourceIterable iterateSource(NubSource source) {
        try {
            return new ClbUsageIteratorNeo(cfg.clb, source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
