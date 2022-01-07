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
package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;

import java.util.HashMap;
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

  public void setDefaultParent(int sourceId, Rank rank, String name) {
    if (sourceById.containsKey(sourceId)) {
      sourceById.get(sourceId).scope = new RankedName(name, rank);
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
