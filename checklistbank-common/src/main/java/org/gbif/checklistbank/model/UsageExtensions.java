package org.gbif.checklistbank.model;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;

import java.util.List;

import com.google.common.collect.Lists;

public class UsageExtensions {
    public List<Description> descriptions = Lists.newArrayList();
    public List<Distribution> distributions = Lists.newArrayList();
    public List<Identifier> identifiers = Lists.newArrayList();
    public List<NameUsageMediaObject> media = Lists.newArrayList();
    public List<Reference> referenceList = Lists.newArrayList();
    public List<SpeciesProfile> speciesProfiles = Lists.newArrayList();
    public List<NameUsage> synonyms = Lists.newArrayList();
    public List<TypeSpecimen> typeSpecimens = Lists.newArrayList();
    public List<VernacularName> vernacularNames = Lists.newArrayList();
}
