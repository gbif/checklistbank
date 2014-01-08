/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package org.gbif.nub.lookup;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nub.utils.RsGbifOrg;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tool to lookup synonyms for higher taxa names. It does a rather strong match and tries to normalize names
 * a lot so we can use them for comparisons of entire higher classifications.
 *
 * The class uses file based dictionaries which are maintained on rs.gbif.org with OpenRefine based on real data found
 * in occurrence and checklist records.
 */
public class HigherTaxaLookup {
  private final static Map<Rank, String> SYNONYM_FILENAMES = Maps.newHashMap();
  static {
    SYNONYM_FILENAMES.put(Rank.KINGDOM, "kingdom.txt");
    SYNONYM_FILENAMES.put(Rank.PHYLUM, "phylum.txt");
    SYNONYM_FILENAMES.put(Rank.CLASS, "class.txt");
    SYNONYM_FILENAMES.put(Rank.ORDER, "order.txt");
    SYNONYM_FILENAMES.put(Rank.FAMILY, "family.txt");
  }
  private static final Set<String> NON_NAMES = Sets.newHashSet();

  private Logger log = LoggerFactory.getLogger(HigherTaxaLookup.class);
  private Map<Rank, Map<String, String>> syn = Maps.newHashMap();
  private Map<String, Kingdom> kingdoms = Maps.newHashMap();

  /**
   *
   */
  public HigherTaxaLookup() {
    for (Kingdom k : Kingdom.values()) {
      this.kingdoms.put(norm(k.name()), k);
    }
  }

  /**
   * Lookup higher taxa synonym dictionary across all ranks and return the first match found
   *
   * @param higherTaxon
   * @return the looked up accepted name or the original higherTaxon
   */
  public String lookup(String higherTaxon) {
    if (higherTaxon == null) {
      return null;
    }
    for (Rank r : syn.keySet()) {
      String result = lookup(higherTaxon, r);
      if (result != null) {
        return result;
      }
    }
    return higherTaxon;
  }

  /**
   * Lookup synonym for given higher rank.
   * Can be null.
   *
   * @param higherTaxon higher rank name, case insensitive
   * @param rank the rank to lookup for
   * @return the looked up accepted name, null for blacklisted names or the original higherTaxon if no synonym is known
   */
  public String lookup(String higherTaxon, Rank rank) {
    if (higherTaxon == null) {
      return null;
    }
    if (isBlacklisted(higherTaxon)) {
      return null;
    }
    if (syn.containsKey(rank)) {
      String normedHT = norm(higherTaxon);
      Map<String, String> x = syn.get(rank);
      if (syn.get(rank).containsKey(normedHT)) {
        return syn.get(rank).get(normedHT);
      }
    }
    return higherTaxon;
  }

  /**
   * Check for obvious, blacklisted garbage and return true if thats the case.
   * The underlying set is hosted at http://rs.gbif.org/dictionaries/authority/blacklisted.txt
   */
  public boolean isBlacklisted(String name) {
    if (name != null) {
      name = norm(name);
      if (NON_NAMES.contains(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return non empty uppercased string with normalized whitespace and all non latin letters replaced. Or null
   */
  @VisibleForTesting
  protected static String norm(String x) {
    Pattern REMOVE_NON_LETTERS = Pattern.compile("[\\W\\d]+");
    x = Strings.nullToEmpty(x);
    x = REMOVE_NON_LETTERS.matcher(x).replaceAll(" ");
    x = StringUtils.normalizeSpace(x).toUpperCase();
    return StringUtils.trimToNull(x);
  }

  private Map<String, String> readSynonymStream(Rank rank, InputStream in) {
    Map<String, String> synonyms = Maps.newHashMap();
    try {
      synonyms = FileUtils.streamToMap(in, 0, 1, true);
    } catch (IOException e) {
      log.warn("Cannot read synonym map from stream for {}. Use empty map instead.", rank, e);
    } finally {
      Closeables.closeQuietly(in);
    }
    log.debug("loaded " + synonyms.size() + " synonyms for " + rank);
    return synonyms;
  }

  /**
   *
   * @param file the synonym file on rs.gbif.org
   * @return
   */
  private Map<String, String> readSynonymUrl(Rank rank, String file) {
    try {
      URL url = RsGbifOrg.synonymUrl(file);
      log.debug("Reading " + url.toString());
      return readSynonymStream(rank, url.openStream());
    } catch (IOException e) {
      log.warn("Cannot read synonym map from " + file + ". Use empty map instead.", e);
    }
    return Maps.newHashMap();
  }

  /**
   *
   * @param file the local file to read
   * @return
   */
  private Map<String, String> readSynonymFile(Rank rank, File file) {
    try {
      log.debug("Reading " + file.getAbsolutePath());
      return readSynonymStream(rank, new FileInputStream(file));
    } catch (IOException e) {
      log.warn("Cannot read synonym map from " + file + ". Use empty map instead: ", e.getMessage());
    }
    return Maps.newHashMap();
  }

  /**
   * Reads blacklisted names from rs.gbif.org
   */
  private void readOnlineBlacklist() {
    try {
      URL url = RsGbifOrg.authorityUrl(RsGbifOrg.FILENAME_BLACKLIST);
      log.debug("Reading " + url.toString());
      readBlacklistStream(url.openStream());
    } catch (IOException e) {
      log.warn("Cannot read online blacklist.", e);
    }
  }

  /**
   * Reads blacklisted names from file
   */
  private void readBlacklistFile(File folder) {
    File blacklist = new File(folder, RsGbifOrg.FILENAME_BLACKLIST);
    try {
      readBlacklistStream(new FileInputStream(blacklist));
    } catch (IOException e) {
      log.warn("Cannot read local blacklist {}. {}", blacklist.getAbsoluteFile(), e.getMessage());
    }
  }

  /**
   * Reads blacklisted names from stream
   */
  private void readBlacklistStream(InputStream in) {
    NON_NAMES.clear();
    try {
      NON_NAMES.addAll(FileUtils.streamToSet(in));
    } catch (IOException e) {
      log.warn("Cannot read blacklist. Use empty set instead.", e);
    } finally {
      Closeables.closeQuietly(in);
    }
    log.debug("loaded " + NON_NAMES.size() + " blacklisted names");
  }

   /**
   * Reads synonym dicts from given folder.
     * File names must be the same as on rs.gbif.org.
   */
  public void loadLocalDicts(File folder) {
    log.info("Reloading dictionary files from rs.gbif.org ...");

    for (Rank rank : SYNONYM_FILENAMES.keySet()) {
      Map<String, String> synonyms = readSynonymFile(rank, new File(folder, SYNONYM_FILENAMES.get(rank)));
      setSynonyms(rank, synonyms);
    }

    // read blacklisted names
    readBlacklistFile(folder);
  }

  /**
   * Reads synonym dicts from given classpath root path.
     * File names must be the same as on rs.gbif.org.
   */
  public void loadClasspathDicts(String classpathFolder) throws IOException {
    log.info("Reloading dictionary files from classpath ...");

    for (Rank rank : SYNONYM_FILENAMES.keySet()) {
      InputStream synIn = Resources.newInputStreamSupplier(Resources.getResource(classpathFolder + "/" + SYNONYM_FILENAMES.get(rank))).getInput();

      Map<String, String> synonyms = readSynonymStream(rank, synIn);
      setSynonyms(rank, synonyms);
    }

    // read blacklisted names
    InputStream blackIn = Resources.newInputStreamSupplier(Resources.getResource(classpathFolder + "/" + RsGbifOrg.FILENAME_BLACKLIST)).getInput();
    readBlacklistStream(blackIn);
  }

  /**
   * Reloads all synonym files found on rs.gbif.org replacing existing mappings.
   */
  public void loadOnlineDicts() {
    log.info("Reloading dictionary files from rs.gbif.org ...");

    for (Rank rank : SYNONYM_FILENAMES.keySet()) {
      Map<String, String> synonyms = readSynonymUrl(rank, SYNONYM_FILENAMES.get(rank));
      setSynonyms(rank, synonyms);
    }

    // read blacklisted names
    readOnlineBlacklist();
  }

  /**
   * Sets the synonym lookup map for a given rank.
   * Names will be normalised and checked for existance of the same entry as key or value.
   *
   * @param rank
   * @param synonyms
   */
  public void setSynonyms(Rank rank, Map<String, String> synonyms) {
    Map<String, String> synonymsNormed = Maps.newHashMap();

    // normalise keys
    for (Entry<String, String> entry : synonyms.entrySet()) {
      synonymsNormed.put(norm(entry.getKey()), entry.getValue());
    }

    // test if synonyms show up as accepted too
    Collection<String> syns = Sets.newHashSet(synonymsNormed.keySet());
    for (String syn : syns) {
      if (synonymsNormed.containsKey(synonymsNormed.get(syn))) {
        log.warn(syn + " is both synonym and accepted - ignore synonym.");
        synonymsNormed.remove(syn);
      }
    }

    syn.put(rank, synonymsNormed);
    log.debug("Loaded " + synonyms.size() + " " + rank.name() + " synonyms ");

    // also create kingdom enum lookup in case of kingdom synonyms
    if (Rank.KINGDOM == rank) {
      Map<String, String> map = syn.get(Rank.KINGDOM);
      if (map != null) {
        for (String syn : map.keySet()) {
          Kingdom k = null;
          String key = map.get(syn);
          if (key != null) {
            key = key.toLowerCase();
            key = StringUtils.capitalize(key);
            try {
              k = Kingdom.valueOf(key);
            } catch (Exception e) {
            }
          }
          this.kingdoms.put(norm(syn), k);
        }
      }
      for (Kingdom k : Kingdom.values()) {
        this.kingdoms.put(norm(k.name()), k);
      }
    }

  }

  /**
   * @return the number of entries across all ranks
   */
  public int size() {
    int all = 0;
    for (Rank r : syn.keySet()) {
      all += size(r);
    }
    return all;
  }

  /**
   * @return the number of entries for a given rank
   */
  public int size(Rank rank) {
    if (syn.containsKey(rank)) {
      return syn.get(rank).size();
    }
    return 0;
  }

  public Kingdom toKingdom(String kingdom) {
    if (kingdom == null) {
      return null;
    }
    return kingdoms.get(kingdom.trim().toUpperCase());
  }

  public Kingdom toKingdomSynonymOnly(String kingdom) {
    if (kingdom == null) {
      return null;
    }
    Kingdom k = kingdoms.get(kingdom.trim().toUpperCase());
    if (k != null && k.name().equalsIgnoreCase(kingdom)) {
      // we have the accepted kingdom name, dont return it
      return null;
    }
    return k;
  }

}
