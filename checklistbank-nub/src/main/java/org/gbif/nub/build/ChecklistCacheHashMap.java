package org.gbif.nub.build;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.checklistbank.model.Usage;
import org.gbif.nub.utils.CacheUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecklistCacheHashMap implements ChecklistCache {
  private static final Logger LOG = LoggerFactory.getLogger(ChecklistCacheHashMap.class);
  private Map<Integer, Usage> uCache;
  private Map<Integer, ParsedName> nCache;

  public ChecklistCacheHashMap () {
    reset();
  }

  @Override
  public void reset(){
    uCache = Maps.newHashMap();
    nCache = Maps.newHashMap();
  }

  @Override
  public void add(Usage usage){
    uCache.put(usage.key, usage);
  }

  /**
   * Verifies that we can add the usage to the cache without breaking its consistency.
   * @throws IllegalStateException if we would reach an inconsistent state
   */
  @Override
  public void checkConsistency(Usage usage){
    if (!nCache.containsKey(usage.nameKey)) {
      throw new IllegalStateException("Name " + usage.nameKey + " not in cache. Cannot add usage");
    }
    if (usage.parentKey > 0 && !uCache.containsKey(usage.parentKey)) {
      throw new IllegalStateException("Parent usage " + usage.parentKey+ " not in cache. Cannot add usage");
    }
  }

  /**
   * @throws IllegalStateException if in inconsistent state
   */
  @Override
  public void checkConsistency(){
    for (Usage u : uCache.values()) {
      checkConsistency(u);
    }
  }

  @Override
  public void add(ParsedName name){
    if (name.getKey() == null) {
      throw new IllegalStateException("A name key cannot be null: " + name.getScientificName());
    }
    if (Strings.isNullOrEmpty(name.getScientificName())) {
      throw new IllegalStateException("A scientific name is required for name " + name.getKey());
    }
    if (!nCache.containsKey(name.getKey())) {
      nCache.put(name.getKey(), name);
    } else {
      LOG.debug("Name key " + name.getKey() + " cached already");
    }
  }

  @Override
  public Usage get(int usageKey) {
    return uCache.get(usageKey);
  }

  /**
   * Nasty implementation - should only be used for small caches as it will iterate over all names and usages
   * to find the requested ones.
   */
  @Override
  public List<Usage> findUsages(String name) {
    Set<Integer> nameKeys = Sets.newHashSet();
    for (ParsedName n : nCache.values()) {
      if (n.getScientificName().equals(name)) {
        nameKeys.add(n.getKey());
      }
    }

    List<Usage> usages = Lists.newArrayList();
    for (Usage u : uCache.values()) {
      if (nameKeys.contains(u.nameKey)) {
        usages.add(u);
      }
    }

    return usages;
  }

  @Override
  public ParsedName getName(int nameKey) {
    return nCache.get(nameKey);
  }

  @Override
  public Iterator<Usage> iterate() {
    return uCache.values().iterator();
  }

  @Override
  public LinneanClassification getClassification(int usageKey) {
    Usage u = get(usageKey);
    if (u != null) {
      LinneanClassification full = new NameUsageMatch();

      // populate higher taxa with canonical names
      if (u.rank != null) {
        ClassificationUtils.setHigherRank(full, u.rank, CacheUtils.canonicalNameOf(u, this));
      }
      while (u.hasParent()) {
        u = uCache.get(u.parentKey);
        if (u.rank != null) {
          ClassificationUtils.setHigherRank(full, u.rank, CacheUtils.canonicalNameOf(u, this));
        }
      }
      return full;
    }
    return null;
  }
}
