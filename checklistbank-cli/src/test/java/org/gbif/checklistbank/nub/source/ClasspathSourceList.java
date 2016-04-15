package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * UsageSource implementation that works on classpath files for testing nub builds.
 * This class is just a test helper class, see NubBuilderTest for its use.
 * For every dataset source there needs to be simple flat usage tab file under resources/nub-sources
 * with the following columns:
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 * <li>usageKey</li>
 * <li>parentKey</li>
 * <li>basionymKey</li>
 * <li>rank (enum)</li>
 * <li>isSynonym (f/t)</li>
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * </ul>
 */
public class ClasspathSourceList extends NubSourceList {
    Map<Integer, NubSource> sourceById = Maps.newHashMap();

    /**
     * Creates a classpath based source that uses no resources at all.
     */
    public static ClasspathSourceList emptySource() {
        return new ClasspathSourceList();
    }

    /**
     * Creates a classpath based source that uses just the specieid classpath resources under /nub-sources
     */
    public static ClasspathSourceList source(Integer ... datasetKeys) {
        List<ClasspathSource> sources = Lists.newArrayList();
        for (Integer id : datasetKeys) {
            sources.add(new ClasspathSource(id));
        }
        return new ClasspathSourceList(sources);
    }

    private ClasspathSourceList() {
        super(false);
    }

    public ClasspathSourceList(List<ClasspathSource> sources) {
        super(false);
        for (ClasspathSource src : sources) {
            sourceById.put(src.id, src);
        }
        submitSources(sources);
    }

    /**
     * Sets the higher rank setting of a given nub source which defaults to family if not set explicitly.
     */
    public void setSourceRank(int sourceId, Rank rank) {
        if (sourceById.containsKey(sourceId)) {
            sourceById.get(sourceId).ignoreRanksAbove = rank;
        }
    }

    public void setNomenclator(int sourceId) {
        if (sourceById.containsKey(sourceId)) {
            sourceById.get(sourceId).nomenclator = true;
        }
    }
}
