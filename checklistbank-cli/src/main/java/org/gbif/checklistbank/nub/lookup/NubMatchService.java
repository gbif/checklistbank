package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.nub.ParentStack;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.ClbSource;
import org.gbif.checklistbank.service.DatasetImportService;

import java.util.Map;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NubMatchService {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchService.class);
  private final ClbConfiguration cfg;
  private final IdLookup nubLookup;
  private final DatasetImportService importService;

  public NubMatchService(ClbConfiguration cfg, IdLookup nubLookup, DatasetImportService importService) {
    this.cfg = cfg;
    this.nubLookup = nubLookup;
    this.importService = importService;
  }

  /**
   * Updates a datasets nub matches.
   * Uses the internal Lookup to generate a complete id map and then does postgres writes in a separate thread ?!
   */
  public void matchDataset(Dataset d) {
    LOG.info("Rematch checklist {} {} to changed backbone", d.getKey(), d.getTitle());
    Map<Integer, Integer> relations = Maps.newHashMap();

    try (ClbSource src = new ClbSource(cfg, d)){
      NubUsage unknown = new NubUsage();
      unknown.usageKey = Kingdom.INCERTAE_SEDIS.nubUsageID();
      unknown.kingdom = Kingdom.INCERTAE_SEDIS;
      // this is a taxonomically sorted iteration. We remember the parent kingdom using the ParentStack
      ParentStack parents = new ParentStack(unknown);
      for (SrcUsage u : src) {
        parents.add(u);
        LookupUsage match = nubLookup.match(u.parsedName.canonicalName(), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, parents.nubKingdom());
        if (match != null) {
          // add to relations
          relations.put(u.key, match.getKey());
          // store current kingdom in parent stack for further nub lookups of children
          NubUsage nub = new NubUsage();
          nub.kingdom = match.getKingdom();
          parents.put(nub);
        } else {
          // also store no matches as nulls so we can flag an issue
          relations.put(u.key, null);
        }
      }
      LOG.info("Updating {} nub relations for dataset {}", relations.size(), d.getKey());
      importService.insertNubRelations(d.getKey(), relations);
    }
  }
}
