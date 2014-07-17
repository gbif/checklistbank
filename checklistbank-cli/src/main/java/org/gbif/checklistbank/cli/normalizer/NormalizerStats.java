package org.gbif.checklistbank.cli.normalizer;

import com.beust.jcommander.internal.Maps;
import org.gbif.api.vocabulary.Origin;

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

    public NormalizerStats() {
        for (Origin o : Origin.values()) {
            countByOrigin.put(o, new AtomicInteger(0));
        }
    }

    public void incOrigin(Origin origin) {
        countByOrigin.get(origin).getAndIncrement();
    }

    public void incRoots(Origin origin) {
        countByOrigin.get(origin).getAndIncrement();
    }

    /**
     * @return raw number of processed source records in dwc archive
     */
    public int getRecords() {
        return records;
    }

    public void setRecords(int records) {
        this.records = records;
        countByOrigin.get(Origin.SOURCE).set(records);
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
            ", countByOrigin=" + countByOrigin +
            '}';
    }
}
