package org.gbif.checklistbank.nub.source;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * </ul>
 * <p>
 * The source files can be generated from a CLB postgres db with the following SQL:
 * SELECT u.id, u.parent_fk, u.basionym_fk, u.rank, u.status, u.nom_status, n.scientific_name from name_usage u join name n on u.name_fk=n.id WHERE ...
 */
public class ClasspathSourceList extends NubSourceList {
  Map<Integer, NubSource> sourceById = Maps.newHashMap();

  /**
   * Creates a classpath based source that uses no resources at all.
   */
  public static ClasspathSourceList emptySource() {
    return new ClasspathSourceList(null, Lists.newArrayList());
  }

  /**
   * Creates a classpath based source that uses just the specieid classpath resources under /nub-sources
   */
  public static ClasspathSourceList source(NeoConfiguration neo, int... datasetKeys) {
    return source(neo, new HashMap<>(), datasetKeys);
  }

  public static ClasspathSourceList source(NeoConfiguration neo, Map<Integer, List<RankedName>> exclusions, int... datasetKeys) {
    List<ClasspathSource> sources = Lists.newArrayList();
    for (Integer id : datasetKeys) {
      sources.add(new ClasspathSource(id, exclusions.get(id), false, neo));
    }
    return new ClasspathSourceList(neo, sources);
  }

  private ClasspathSourceList(NeoConfiguration neo, Iterable<ClasspathSource> sources) {
    super(new NubConfiguration(neo));
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

  public void setSourceIgnoreSynonyms(int sourceId, boolean ignoreSynonyms) {
    if (sourceById.containsKey(sourceId)) {
      sourceById.get(sourceId).ignoreSynonyms = ignoreSynonyms;
    }
  }

  public void setNomenclator(int sourceId) {
    if (sourceById.containsKey(sourceId)) {
      sourceById.get(sourceId).nomenclator = true;
    }
  }
  
  public void setSupragenericHomonymSource(int sourceId) {
    if (sourceById.containsKey(sourceId)) {
      sourceById.get(sourceId).supragenericHomonymSource = true;
    }
  }

  public void includeOTUs(int sourceId) {
    if (sourceById.containsKey(sourceId)) {
      sourceById.get(sourceId).includeOTUs = true;
    }
  }

  public void setNameTypeMapping(int sourceId, Map<NameType, NameType> mapping) {
    if (sourceById.containsKey(sourceId)) {
      sourceById.get(sourceId).nameTypeMapping = mapping;
    }
  }
}
