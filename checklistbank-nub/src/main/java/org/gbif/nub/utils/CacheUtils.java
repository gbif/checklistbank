package org.gbif.nub.utils;

import org.gbif.checklistbank.service.mybatis.model.Usage;
import org.gbif.nub.build.ChecklistCache;

public class CacheUtils {

  private CacheUtils () {
  }

  public static String nameOf(Usage u, ChecklistCache cache) {
    return cache.getName(u.nameKey).getScientificName();
  }

  public static String canonicalNameOf(Usage u, ChecklistCache cache) {
    return cache.getName(u.nameKey).canonicalName();
  }
}
