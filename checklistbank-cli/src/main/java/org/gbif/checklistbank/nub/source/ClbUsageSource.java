package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.util.MachineTagUtils;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.nub.model.NubTags;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.List;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClbUsageSource implements UsageSource {

  private static final Logger LOG = LoggerFactory.getLogger(ClbUsageSource.class);
  private List<NubSource> sources;
  private final DatasetService datasetService;
  private final ClbConfiguration clbCfg;

  public ClbUsageSource(NubConfiguration cfg) {
    this.clbCfg = cfg.clb;
    LOG.info("Loading backbone sources from registry {}", cfg.registry.wsUrl);
    Injector regInj = cfg.registry.createRegistryInjector();
    datasetService = regInj.getInstance(DatasetService.class);
  }

  public ClbUsageSource(DatasetService datasetService, ClbConfiguration cfg) {
    this.datasetService = datasetService;
    clbCfg = cfg;
  }

  @Override
  public List<NubSource> listSources() {
    if (sources == null) {
      loadSourcesFromRegistry();
    }
    return sources;
  }

  private void loadSourcesFromRegistry() {

    PagingRequest req = new PagingRequest(0, 100);
    PagingResponse<Dataset> resp = null;
    sources = Lists.newArrayList();
    while (resp == null || !resp.isEndOfRecords()) {
      resp = datasetService.listByType(DatasetType.CHECKLIST, req);
      for (Dataset d : resp.getResults()) {
        MachineTag priority = MachineTagUtils.firstTag(d, NubTags.NAMESPACE, NubTags.PRIORITY.tag);
        if (priority != null) {
          NubSource src = new NubSource();
          src.key = d.getKey();
          src.name = d.getTitle();
          try {
            src.priority = Integer.valueOf(priority.getValue());
            sources.add(src);
            MachineTag rank = MachineTagUtils.firstTag(d, NubTags.NAMESPACE, NubTags.RANK_LIMIT.tag);
            if (rank != null) {
              src.ignoreRanksAbove = VocabularyUtils.lookupEnum(rank.getValue(), Rank.class);
            }
          } catch (NumberFormatException e) {
            LOG.warn("Bad backbone priority for dataset {} is not an integer: {}. Ignore", d.getKey(), priority.getValue());
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
      return new ClbUsageIteratorNeo(clbCfg, source);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
