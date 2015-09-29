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
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.iterable.FutureIterator;
import org.gbif.io.CSVReader;
import org.gbif.utils.file.FileUtils;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * A source for nub sources backed by usage data from checklistbank.
 * The list of source datasets is discovered by reading a configured tab delimited online file.
 *
 * The sources are then loaded asynchroneously through a single background thread into temporary neo4j databases.
 */
public class ClbSourceList implements CloseableIterable<NubSource> {

    private static final Logger LOG = LoggerFactory.getLogger(ClbSourceList.class);
    private List<Future<NubSource>> futures = Lists.newArrayList();
    private final DatasetService datasetService;
    private final OrganizationService organizationService;
    private final NubConfiguration cfg;
    private final ExecutorService exec;

    public static ClbSourceList create(NubConfiguration cfg) {
        Injector regInj = cfg.registry.createRegistryInjector();
        return new ClbSourceList(regInj.getInstance(DatasetService.class), regInj.getInstance(OrganizationService.class), cfg);
    }

    public ClbSourceList(DatasetService datasetService, OrganizationService organizationService, NubConfiguration cfg) {
        exec = Executors.newSingleThreadExecutor();
        this.datasetService = datasetService;
        this.organizationService = organizationService;
        this.cfg = cfg;
        loadSources();
    }

    private NubSource buildSource(Dataset d, int priority, Rank rank) {
        NubSource src = new ClbSource(cfg.clb,d.getKey(), d.getTitle());
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
        List<NubSource> sources = Lists.newArrayList();

        LOG.info("Loading backbone sources from {}", cfg.sourceList);
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
                    sources.add(buildSource(d, priority, rank));

                } else {
                    // try if its an organization
                    Organization org = organizationService.get(key);
                    if (org == null) {
                        LOG.warn("Unknown nub source {}. Ignore", key);
                    } else {
                        int counter = 0;
                        for (Dataset d2 : Iterables.publishedDatasets(org.getKey(), DatasetType.CHECKLIST, organizationService)) {
                            if (!keys.contains(d2.getKey())) {
                                sources.add(buildSource(d2, priority, rank));
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
        // submit loader jobs
        for (NubSource src : sources) {
            futures.add(exec.submit(new LoadSource(src)));
        }
    }

    @Override
    public Iterator<NubSource> iterator() {
        return new FutureIterator<NubSource>(futures);
    }

    @Override
    public void close() throws Exception {
        exec.shutdownNow();
    }
}
