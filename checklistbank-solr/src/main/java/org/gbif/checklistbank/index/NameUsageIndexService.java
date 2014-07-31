package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Real time solr service for checklistbank.
 */
public interface NameUsageIndexService {

  void delete(int usageKey);

  void delete(UUID datasetKey);

  void insertOrUpdate(Collection<Integer> usageKeys);

  void insertOrUpdate(NameUsage usage, List<VernacularName> vernaculars,
    List<Description> descriptions, List<Distribution> distributions, List<SpeciesProfile> profiles);
}
