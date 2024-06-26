/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.nub.ParentStack;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.ClbSource;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.utils.NameFormatter;
import org.gbif.nub.lookup.straight.DatasetMatchFailed;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class NubMatchService {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchService.class);
  protected final ClbConfiguration cfg;
  private final NeoConfiguration neo;
  protected IdLookup nubLookup;
  private final DatasetImportService sqlService;
  private final DatasetImportService searchIndexService;
  private int counter = 0;

  public NubMatchService(ClbConfiguration cfg, NeoConfiguration neo, IdLookup nubLookup, DatasetImportService sqlService, DatasetImportService searchIndexService) {
    this.cfg = cfg;
    this.neo = neo;
    this.nubLookup = nubLookup;
    this.sqlService = sqlService;
    this.searchIndexService = searchIndexService;
  }

  /**
   * @return number of checklists that have been matched so far.
   */
  public int getCounter() {
    return counter;
  }

  /**
   * Updates a datasets nub matches.
   * Uses the internal Lookup to generate a complete id map and then does postgres writes in a separate thread ?!
   */
  public DatasetMatchSummary matchDataset(UUID key) throws DatasetMatchFailed {
    Dataset d = new Dataset();
    d.setKey(key);
    d.setTitle("Dataset " + key);

    final DatasetMatchSummary summary = new DatasetMatchSummary(d.getKey());

    if (Constants.NUB_DATASET_KEY.equals(d.getKey())) {
      LOG.warn("Cannot match backbone to itself. Ignore");
      return summary;
    }

    LOG.info("Rematch checklist {} to Backbone", d.getKey());
    Map<Integer, Integer> relations = Maps.newHashMap();
    try (ClbSource src = new ClbSource(cfg, neo, d, null)) {
      // read in postgres usages
      LOG.info("Copy usages for {} from pg into neo", d.getKey());
      src.init(false, false);

      NubUsage unknown = new NubUsage();
      unknown.usageKey = Kingdom.INCERTAE_SEDIS.nubUsageKey();
      unknown.kingdom = Kingdom.INCERTAE_SEDIS;
      // this is a taxonomically sorted iteration. We remember the parent kingdom using the ParentStack
      ParentStack parents = new ParentStack(unknown);
      try (CloseableIterator<SrcUsage> iter = src.iterator()) {
        while (iter.hasNext()) {
          SrcUsage u = iter.next();
          parents.add(u);
        
          if (u.parsedName.isParsableType() && !u.parsedName.isParsed()) {
            summary.addUnparsable();
          }

          // ignore status when matching to backbone!!!
          LookupUsage match;
          if (u.parsedName.isParsed()) {
            // match by canonically reconstructed name
            match = nubLookup.match(NameFormatter.canonicalOrScientificName(u.parsedName), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, null, parents.nubKingdom());
          } else {
            // match by full sciname
            match = nubLookup.match(u.scientificName, u.rank, parents.nubKingdom());
          }

          if (match != null) {
            summary.addMatch(u.rank);
            // add to relations
            relations.put(u.key, match.getKey());
            // store current kingdom in parent stack for further nub lookups of children
            NubUsage nub = new NubUsage();
            nub.kingdom = match.getKingdom();
            parents.put(nub);

          } else {
            summary.addNoMatch(u.rank);
            LOG.debug("No match for {} in dataset {}. Parsed name: {}", u, d.getKey(), u.parsedName);
            // also store no matches as nulls so we can flag an issue
            relations.put(u.key, null);
          }
        }
      }

      // warn if matches are little
      // but ignore very small datasets where chances are high due to higher taxa often not matching
      if (summary.percBackboneRelevantNoMatches() < 25 || (summary.percMatches() < 25 && summary.getTotalUsages() > 25)) {
        LOG.warn("Only {}% of all names and {}% of genera and below in dataset {} were matching", summary.percMatches(), summary.percBackboneRelevantNoMatches(), d.getKey());
      }

      if (sqlService != null) {
        LOG.info("Updating {} nub relations with {} ({}%) matches and {} unparsable names from dataset {}",
            relations.size(),
            summary.getMatches(),
            summary.percMatches(),
            summary.getUnparsable(),
            d.getKey()
        );
        sqlService.insertNubRelations(d.getKey(), relations);
        if (searchIndexService != null) {
          LOG.warn("No SOLR service configured to update matches in search index for dataset {}!", d.getKey());
          searchIndexService.insertNubRelations(d.getKey(), relations);
        }
        counter++;
      } else {
        LOG.warn("No sql service configured to persist the matches for dataset {}!", d.getKey());
      }

    } catch (Exception e) {
      LOG.error("Failed to match checklist {} {}", d.getKey(), d.getTitle());
      throw new DatasetMatchFailed(d.getKey(), e);
    }

    LOG.info("{}", summary);
    return summary;
  }

}
