package org.gbif.checklistbank.nub;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.util.MachineTagUtils;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;

import java.util.List;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClbUsageSource implements UsageSource {

  private static final Logger LOG = LoggerFactory.getLogger(ClbUsageSource.class);
  private final NubConfiguration cfg;
  private List<NubSource> sources;

  public ClbUsageSource(NubConfiguration cfg) {
    this.cfg = cfg;
    sources = cfg.getSources();
  }

  @Override
  public List<NubSource> listSources() {
    if (sources.isEmpty()) {
      loadSourcesFromRegistry();
    }
    return sources;
  }

  private void loadSourcesFromRegistry() {
    LOG.info("Loading backbone sources from registry {}", cfg.registry.wsUrl);
    Injector regInj = cfg.registry.createRegistryInjector();
    final DatasetService datasetService = regInj.getInstance(DatasetService.class);

    PagingRequest req = new PagingRequest(0, 100);
    PagingResponse<Dataset> resp = null;

    while (resp == null || !resp.isEndOfRecords()) {
      resp = datasetService.listByType(DatasetType.CHECKLIST, req);
      for (Dataset d : resp.getResults()) {
        MachineTag priority = MachineTagUtils.firstTag(d, NubTags.NAMESPACE, NubTags.PRIORITY.tag);
        if (priority != null) {
          NubSource src = new NubSource();
          src.key = d.getKey();
          src.name = d.getTitle();
          sources.add(src);
          MachineTag rank = MachineTagUtils.firstTag(d, NubTags.NAMESPACE, NubTags.RANK_LIMIT.tag);
          if (rank != null) {
            src.ignoreRanksAbove = VocabularyUtils.lookupEnum(rank.getValue(), Rank.class);
          }
        }
      }
      req.nextPage();
    }
    LOG.info("Found {} tagged backbone sources in the registry", sources.size());

    // sort source according to priority
    Ordering<NubSource> order = Ordering.natural().onResultOf(new Function<NubSource, Integer>() {
      @Nullable
      @Override
      public Integer apply(NubSource input) {
        return input.priority;
      }
    });
    sources = order.sortedCopy(sources);
  }

  @Override
  public Iterable<SrcUsage> iterateSource(NubSource source) {
    try {
      return new ClbUsageIteratorNeo(cfg.clb, source);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
