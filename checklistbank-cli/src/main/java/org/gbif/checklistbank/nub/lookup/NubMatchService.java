package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.nub.ParentStack;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.ClbSource;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.gbif.nub.lookup.straight.DatasetMatchFailed;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NubMatchService {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchService.class);
  protected final ClbConfiguration cfg;
  protected IdLookup nubLookup;
  private final DatasetImportService sqlService;
  private final DatasetImportService solrService;
  private final MessagePublisher publisher;
  private int counter = 0;

  public NubMatchService(ClbConfiguration cfg, IdLookup nubLookup, DatasetImportService sqlService, DatasetImportService solrService, MessagePublisher publisher) {
    this.cfg = cfg;
    this.nubLookup = nubLookup;
    this.sqlService = sqlService;
    this.solrService = solrService;
    this.publisher = publisher;
  }

  public int getCounter() {
    return counter;
  }

  /**
   * Updates a datasets nub matches.
   * Uses the internal Lookup to generate a complete id map and then does postgres writes in a separate thread ?!
   */
  public void matchDataset(Dataset d) throws DatasetMatchFailed {
    if (Constants.NUB_DATASET_KEY.equals(d.getKey())) {
      LOG.warn("Cannot match backbone to itself. Ignore");
      return;
    }

    LOG.info("Rematch checklist {} to Backbone", d.getKey());
    Map<Integer, Integer> relations = Maps.newHashMap();

    try (ClbSource src = new ClbSource(cfg, d)){
      // read in postgres usages
      LOG.info("Copy usages for {} from pg into neo", d.getKey());
      src.init(false, false, true);

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
      sqlService.insertNubRelations(d.getKey(), relations);
      solrService.insertNubRelations(d.getKey(), relations);
      counter++;

      //ChecklistSyncedMessage triggers a new dataset analysis
      LOG.info("Sending {} for dataset {} {}", ChecklistSyncedMessage.class.getSimpleName(), d.getKey(), d.getTitle());
      publisher.send(new ChecklistSyncedMessage(d.getKey(), new Date(), 1, 0));

    } catch (Exception e) {
      LOG.error("Failed to match checklist {} {}", d.getKey(), d.getTitle());
      throw new DatasetMatchFailed(d.getKey(), e);
    }
  }

  public void matchDataset(UUID key) throws DatasetMatchFailed {
    Dataset d = new Dataset();
    d.setKey(key);
    d.setTitle("Dataset "+key);
    matchDataset(d);
  }
}
