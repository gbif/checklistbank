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
package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.model.Classification;
import org.gbif.checklistbank.neo.UsageDao;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Builds higher classification keys (not the verbatim names) and NameUsageMetrics for all accepted usages.
 * Synonym usages do not need a data record as its zero all over.
 * The handler works on taxonomic neo relations and the NameUsage instances in the kvp store, so make sure they exist!
 */
public class UsageMetricsHandler implements StartEndHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UsageMetricsHandler.class);
    // neo node ids for the higher classification links
    private final Classification classification = new Classification();
    private final LinkedList<NameUsageMetrics> parentCounts = Lists.newLinkedList();
    private int counter;
    private int roots;
    private int maxDepth;
    private int depth;
    private int synonyms;
    private Map<Origin, Integer> countByOrigin = Maps.newHashMap();
    private Map<Rank, Integer> countByRank = Maps.newHashMap();
    private final UsageDao dao;
    private final boolean debug = false;

    public UsageMetricsHandler(UsageDao dao) {
        this.dao = dao;
    }

    @Override
    public void start(Node n) {
        NameUsage u = dao.readUsage(n, false);
        Preconditions.checkNotNull(u, "node " + n.getId() + " with missing name usage found");
        // increase counters
        count(u);
        counter++;
        depth++;
        if (depth > maxDepth) {
            maxDepth = depth;
        }
        if (depth == 1) {
            roots++;
        }
        if (u.getRank() != null && u.getRank().isLinnean()) {
            ClassificationUtils.setHigherRankKey(classification, u.getRank(), (int) n.getId());
            ClassificationUtils.setHigherRank(classification, u.getRank(), u.getCanonicalOrScientificName());
        }
        // for linnean ranks increase all parent data
        if (u.getRank() != null && u.getRank().isLinnean() && u.getRank() != Rank.KINGDOM) {
            for (NameUsageMetrics m : parentCounts) {
                setNumByRank(m, u.getRank(), m.getNumByRank(u.getRank()) + 1);
            }
        }
        // increase direct parents children counter by one
        if (!parentCounts.isEmpty()) {
            parentCounts.getLast().setNumChildren(parentCounts.getLast().getNumChildren() + 1);
        }
        // add new data to list of parent data
        NameUsageMetrics m = new NameUsageMetrics();
        // keep current total counter state so we can calculate the difference for the num descendants when coming up again
        m.setNumDescendants(counter);
        parentCounts.add(m);
        if (debug) LOG.info("start: {} {} {} #  {}-0-{}", u.getTaxonID(), u.getRank(), u.getScientificName(), counter, parentCounts.size());
    }

    @Override
    public void end(Node n) {
        depth--;
        NameUsage u = dao.readUsage(n, false);
        // final data update
        NameUsageMetrics metrics = parentCounts.removeLast();
        metrics.setNumSynonyms(processSynonyms(n));
        metrics.setNumDescendants(counter - metrics.getNumDescendants());

        // persist data and classification with nub key
        UsageFacts facts = new UsageFacts();
        facts.metrics = metrics;
        facts.classification = classification;
        // add this rank AGAIN to its current classification to fix an issue with parents being the same rank
        // see https://github.com/gbif/checklistbank/issues/161
        // we will remove it again after we persisted it
        if (u.getRank() != null && u.getRank().isLinnean()) {
            ClassificationUtils.setHigherRankKey(classification, u.getRank(), (int) n.getId());
            ClassificationUtils.setHigherRank(classification, u.getRank(), u.getCanonicalOrScientificName());
        }
        dao.store(n.getId(), facts);

        // remove this rank from current classification
        if (u.getRank() != null && u.getRank().isLinnean()) {
            ClassificationUtils.setHigherRankKey(classification, u.getRank(), null);
            ClassificationUtils.setHigherRank(classification, u.getRank(), null);
        }
        if (debug) LOG.info("end: {} {} {} #  {}-{}-{}", u.getTaxonID(), u.getRank(), u.getScientificName(), metrics.getNumDescendants(), metrics.getNumSynonyms(), parentCounts.size());
    }

    public NormalizerStats getStats(int ignored, List<String> cycles) {
        return new NormalizerStats(roots, maxDepth, synonyms, ignored, countByOrigin, countByRank, cycles);
    }

    /**
     * Process all synonymsTD doing a nub lookup for each of them
     *
     * @return the number of processed synonymsTD
     */
    private int processSynonyms(Node n) {
        int synCounter = 0;
        for (Node syn : Traversals.SYNONYMS.traverse(n).nodes()) {
            NameUsage u = dao.readUsage(syn, false);
            synCounter++;
            count(u);
        }
        synonyms = synonyms + synCounter;
        return synCounter;
    }

    private static void setNumByRank(NameUsageMetrics u, Rank rank, int count) {
        if (rank == Rank.PHYLUM) {
            u.setNumPhylum(count);
        }
        if (rank == Rank.CLASS) {
            u.setNumClass(count);
        }
        if (rank == Rank.ORDER) {
            u.setNumOrder(count);
        }
        if (rank == Rank.FAMILY) {
            u.setNumFamily(count);
        }
        if (rank == Rank.GENUS) {
            u.setNumGenus(count);
        }
        if (rank == Rank.SUBGENUS) {
            u.setNumSubgenus(count);
        }
        if (rank == Rank.SPECIES) {
            u.setNumSpecies(count);
        }
    }

    private void count(NameUsage u) {
        if (u.getOrigin() != null) {
            if (!countByOrigin.containsKey(u.getOrigin())) {
                countByOrigin.put(u.getOrigin(), 1);
            } else {
                countByOrigin.put(u.getOrigin(), countByOrigin.get(u.getOrigin()) + 1);
            }
        }
        if (u.getRank() != null) {
            if (!countByRank.containsKey(u.getRank())) {
                countByRank.put(u.getRank(), 1);
            } else {
                countByRank.put(u.getRank(), countByRank.get(u.getRank()) + 1);
            }
        }
    }

}
