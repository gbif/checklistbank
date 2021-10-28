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
import java.util.Objects;

import com.google.common.collect.Lists;

public class UsageExtensions {
    public List<Description> descriptions = Lists.newArrayList();
    public List<Distribution> distributions = Lists.newArrayList();
    public List<Identifier> identifiers = Lists.newArrayList();
    public List<NameUsageMediaObject> media = Lists.newArrayList();
    public List<Reference> referenceList = Lists.newArrayList();
    public List<SpeciesProfile> speciesProfiles = Lists.newArrayList();
    public List<TypeSpecimen> typeSpecimens = Lists.newArrayList();
    public List<VernacularName> vernacularNames = Lists.newArrayList();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsageExtensions that = (UsageExtensions) o;
        return Objects.equals(descriptions, that.descriptions) &&
                Objects.equals(distributions, that.distributions) &&
                Objects.equals(identifiers, that.identifiers) &&
                Objects.equals(media, that.media) &&
                Objects.equals(referenceList, that.referenceList) &&
                Objects.equals(speciesProfiles, that.speciesProfiles) &&
                Objects.equals(typeSpecimens, that.typeSpecimens) &&
                Objects.equals(vernacularNames, that.vernacularNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptions, distributions, identifiers, media, referenceList, speciesProfiles, typeSpecimens, vernacularNames);
    }
}
