package org.gbif.checklistbank.cli.normalizer;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class NormalizerStats {
    private int records;
    private int roots;
    private int depth;
    private int synonyms;
    private final Map<Origin, AtomicInteger> countByOrigin = Maps.newHashMap();
    private final Map<Rank, AtomicInteger> countByRank = Maps.newHashMap();
    private List<String> cycles = Lists.newArrayList();

    /**
     * @return list of cycles, each given as one taxonID of the loop
     */
    public List<String> getCycles() {
        return cycles;
    }

    public void incOrigin(Origin origin) {
        if (origin != null) {
            if (!countByOrigin.containsKey(origin)) {
                countByOrigin.put(origin, new AtomicInteger(1));
            } else {
                countByOrigin.get(origin).getAndIncrement();
            }
        }
    }

    public void incRank(Rank rank) {
        if (rank != null) {
            if (!countByRank.containsKey(rank)) {
                countByRank.put(rank, new AtomicInteger(1));
            } else {
                countByRank.get(rank).getAndIncrement();
            }
        }
    }

    /**
     * @return raw number of processed source records in dwc archive
     */
    public int getRecords() {
        return records;
    }

    public void setRecords(int records) {
        this.records = records;
        countByOrigin.put(Origin.SOURCE, new AtomicInteger(records));
    }

    /**
     * @return total number of name usages existing as neo nodes, both accepted and synonyms
     */
    public int getUsages() {
        int total = 0;
        for (AtomicInteger x : countByOrigin.values()) {
            total += x.get();
        }
        return total;
    }

    /**
     * @return the number of root taxa without a parent
     */
    public int getRoots() {
        return roots;
    }

    public void setRoots(int roots) {
        this.roots = roots;
    }

    /**
     * @return maximum depth of the classification
     */
    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * @return the number of synonym nodes in this checklist
     */
    public int getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(int synonyms) {
        this.synonyms = synonyms;
    }

    public int getCountByRank(Rank rank) {
        if (countByRank.containsKey(rank)) {
            return countByRank.get(rank).get();
        }
        return 0;
    }

    public int getCountByOrigin(Origin o) {
        return countByOrigin.get(o).get();
    }

    public Map<Origin, Integer> getCountByOrigin() {
        Map<Origin, Integer> counts = Maps.newHashMap();
        for (Map.Entry<Origin, AtomicInteger> entry : countByOrigin.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().intValue());
        }
        return counts;
    }

    @Override
    public String toString() {
        return "NormalizerStats{" +
            "records=" + records +
            ", roots=" + roots +
            ", depth=" + depth +
            ", synonyms=" + synonyms +
            ", cycles=" + cycles.size() +
            ", countByOrigin=" + countByOrigin +
            ", countByRank=" + countByRank +
            '}';
    }
}
