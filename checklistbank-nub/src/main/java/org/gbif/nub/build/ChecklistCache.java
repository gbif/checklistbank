package org.gbif.nub.build;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.Usage;
import org.gbif.nub.lookup.ClassificationResolver;

import java.util.Iterator;
import java.util.List;

public interface ChecklistCache extends ClassificationResolver {

  void reset();

  void add(Usage usage);

  void add(ParsedName name);

  Usage get(int usageKey);

  List<Usage> findUsages(String name);

  ParsedName getName(int nameKey);

  Iterator<Usage> iterate();

  void checkConsistency(Usage usage);

  void checkConsistency();
}
