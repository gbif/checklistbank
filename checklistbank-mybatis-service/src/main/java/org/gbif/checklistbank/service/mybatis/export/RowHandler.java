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
package org.gbif.checklistbank.service.mybatis.export;

import org.gbif.api.model.checklistbank.*;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.model.ParsedNameUsage;
import org.gbif.dwc.DwcaStreamWriter;
import org.gbif.dwc.terms.*;

import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

abstract class RowHandler<T> implements Consumer<T>, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(RowHandler.class);
  private static final Joiner CONCAT = Joiner.on(";").skipNulls();

  private final DwcaStreamWriter.RowWriteHandler writer;
  private int counter;
  private final Term rowType;

  public RowHandler(DwcaStreamWriter writer, Term rowType, List<Term> columns) {
    int idx = 1;
    Map<Term, Integer> mapping = Maps.newHashMap();
    for (Term term : columns) {
      mapping.put(term, idx++);
    }
    this.writer = writer.writeHandler(rowType, 0, mapping);
    this.rowType = rowType;
  }

  abstract String[] toRow(T obj);

  @Override
  public void accept(T result) {
    writer.write(toRow(result));
    if (counter++ % 100000 == 0) {
      LOG.debug("{} {} records added to dwca", counter, rowType.simpleName());
    }
  }

  public int getCounter() {
    return counter;
  }

  @Override
  public void close() throws Exception {
    writer.close();
  }

  private static String toStr(Collection<? extends Enum> es) {
    if (es == null) return "";
    return CONCAT.join(es).toLowerCase().replaceAll("_", " ");
  }

  private static String toStr(Language l) {
    if (l == null) return null;
    return l.getIso2LetterCode();
  }

  private static String toStr(Enum e) {
    if (e == null) return null;
    return e.name().toLowerCase().replaceAll("_", " ");
  }

  private static String toStr(Date date) {
    if (date == null) return null;
    return DateFormatUtils.ISO_DATE_FORMAT.format(date);
  }

  private static String toStr(Object obj) {
    return obj == null ? null : obj.toString();
  }

  private static void addCountryColumns(String[] row, int idx, Country val) {
    if (val != null) {
      row[idx++] = val.getTitle();
      row[idx] = val.getIso2LetterCode();
    } else {
      row[idx++] = null;
      row[idx] = null;
    }
  }


  static class TaxonHandler extends RowHandler<ParsedNameUsage> {

    static final List<Term> columns = ImmutableList.of(
            DwcTerm.datasetID,
            DwcTerm.parentNameUsageID,
            DwcTerm.acceptedNameUsageID,
            DwcTerm.originalNameUsageID,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            GbifTerm.canonicalName,
            DwcTerm.genericName,
            DwcTerm.specificEpithet,
            DwcTerm.infraspecificEpithet,
            DwcTerm.taxonRank,
            DwcTerm.nameAccordingTo,
            DwcTerm.namePublishedIn,
            DwcTerm.taxonomicStatus,
            DwcTerm.nomenclaturalStatus,
            DwcTerm.taxonRemarks,
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus
    );
    private final Set<UUID> constituents = Sets.newHashSet();
    private final UUID datasetKey;

    TaxonHandler(DwcaStreamWriter writer, UUID datasetKey) {
      super(writer, DwcTerm.Taxon, columns);
      this.datasetKey = datasetKey;
    }

    public Set<UUID> getConstituents() {
      return constituents;
    }

    @Override
    String[] toRow(ParsedNameUsage u) {
      String[] row = new String[columns.size()+1];

      final ParsedName pn = u.getParsedName();

      int idx = 0;
      row[idx++] = toStr(u.getKey());
      row[idx++] = toStr(u.getConstituentKey());
      if (u.getConstituentKey() != null && !u.getConstituentKey().equals(datasetKey)) {
        constituents.add(u.getConstituentKey());
      }
      row[idx++] = toStr(u.getParentKey());
      row[idx++] = toStr(u.getAcceptedKey());
      row[idx++] = toStr(u.getBasionymKey());
      // name
      row[idx++] = u.getScientificName();
      row[idx++] = u.getAuthorship();
      row[idx++] = u.getCanonicalName();
      row[idx++] = pn.getGenusOrAbove();
      row[idx++] = pn.getSpecificEpithet();
      row[idx++] = pn.getInfraSpecificEpithet();
      // taxon
      row[idx++] = toStr(u.getRank());
      row[idx++] = u.getAccordingTo();
      row[idx++] = u.getPublishedIn();
      row[idx++] = toStr(u.getTaxonomicStatus());
      row[idx++] = toStr(u.getNomenclaturalStatus());
      row[idx++] = u.getRemarks();
      // classification
      row[idx++] = u.getKingdom();
      row[idx++] = u.getPhylum();
      row[idx++] = u.getClazz();
      row[idx++] = u.getOrder();
      row[idx++] = u.getFamily();
      row[idx] = u.getGenus();

      return row;
    }
  }

  static class DescriptionHandler extends RowHandler<Description> {

    static final List<Term> columns = ImmutableList.of(
            DcTerm.type,
            DcTerm.language,
            DcTerm.description,
            DcTerm.source,
            DcTerm.creator,
            DcTerm.contributor,
            DcTerm.license
    );

    DescriptionHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.Description, columns);
    }

    @Override
    String[] toRow(Description d) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(d.getTaxonKey());
      row[idx++] = d.getType();
      row[idx++] = toStr(d.getLanguage());
      row[idx++] = d.getDescription();
      row[idx++] = d.getSource();
      row[idx++] = d.getCreator();
      row[idx++] = d.getContributor();
      row[idx] = d.getLicense();

      return row;
    }
  }

  static class DistributionHandler extends RowHandler<Distribution> {

    static final List<Term> columns = ImmutableList.of(
            DwcTerm.locationID,
            DwcTerm.locality,
            DwcTerm.country,
            DwcTerm.countryCode,
            DwcTerm.locationRemarks,
            DwcTerm.establishmentMeans,
            DwcTerm.lifeStage,
            DwcTerm.occurrenceStatus,
            IucnTerm.threatStatus,
            DcTerm.source
    );

    DistributionHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.Distribution, columns);
    }

    @Override
    String[] toRow(Distribution d) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(d.getTaxonKey());
      row[idx++] = d.getLocationId();
      row[idx++] = d.getLocality();
      addCountryColumns(row, idx, d.getCountry());
      idx = idx + 2;
      row[idx++] = d.getRemarks();
      row[idx++] = toStr(d.getEstablishmentMeans());
      row[idx++] = toStr(d.getLifeStage());
      row[idx++] = toStr(d.getStatus());
      row[idx++] = toStr(d.getThreatStatus());
      row[idx] = d.getSource();

      return row;
    }
  }

  static class NameUsageMediaObjectHandler extends RowHandler<NameUsageMediaObject> {

    static final List<Term> columns = ImmutableList.of(
            DcTerm.identifier,
            DcTerm.references,
            DcTerm.title,
            DcTerm.description,
            DcTerm.license,
            DcTerm.creator,
            DcTerm.created,
            DcTerm.contributor,
            DcTerm.publisher,
            DcTerm.rightsHolder,
            DcTerm.source
    );

    NameUsageMediaObjectHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.Multimedia, columns);
    }

    @Override
    String[] toRow(NameUsageMediaObject m) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(m.getTaxonKey());
      row[idx++] = toStr(m.getIdentifier());
      row[idx++] = toStr(m.getReferences());
      row[idx++] = m.getTitle();
      row[idx++] = m.getDescription();
      row[idx++] = m.getLicense();
      row[idx++] = m.getCreator();
      row[idx++] = toStr(m.getCreated());
      row[idx++] = m.getContributor();
      row[idx++] = m.getPublisher();
      row[idx++] = m.getRightsHolder();
      row[idx] = m.getSource();

      return row;
    }
  }

  static class ReferenceHandler extends RowHandler<Reference> {

    static final List<Term> columns = ImmutableList.of(
            DcTerm.bibliographicCitation,
            DcTerm.identifier,
            DcTerm.references,
            DcTerm.source
    );

    ReferenceHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.Reference, columns);
    }

    @Override
    String[] toRow(Reference r) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(r.getTaxonKey());
      row[idx++] = r.getCitation();
      row[idx++] = r.getDoi();
      row[idx++] = r.getLink();
      row[idx] = r.getSource();

      return row;
    }
  }

  static class TypeSpecimenHandler extends RowHandler<TypeSpecimen> {

    static final List<Term> columns = ImmutableList.of(
            GbifTerm.typeDesignationType,
            GbifTerm.typeDesignatedBy,
            DwcTerm.scientificName,
            DwcTerm.taxonRank,
            DcTerm.source
    );

    TypeSpecimenHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.TypesAndSpecimen, columns);
    }

    @Override
    String[] toRow(TypeSpecimen t) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(t.getTaxonKey());
      row[idx++] = toStr(t.getTypeDesignationType());
      row[idx++] = t.getTypeDesignatedBy();
      row[idx++] = t.getScientificName();
      row[idx++] = toStr(t.getTaxonRank());
      row[idx] = t.getSource();

      return row;
    }
  }

  static class VernacularNameHandler extends RowHandler<VernacularName> {

    static final List<Term> columns = ImmutableList.of(
            DwcTerm.vernacularName,
            DcTerm.language,
            DwcTerm.country,
            DwcTerm.countryCode,
            DwcTerm.sex,
            DwcTerm.lifeStage,
            DcTerm.source
    );

    VernacularNameHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.VernacularName, columns);
    }

    @Override
    String[] toRow(VernacularName v) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(v.getTaxonKey());
      row[idx++] = v.getVernacularName();
      row[idx++] = toStr(v.getLanguage());
      addCountryColumns(row, idx, v.getCountry());
      idx=idx+2;
      row[idx++] = toStr(v.getSex());
      row[idx++] = toStr(v.getLifeStage());
      row[idx] = v.getSource();

      return row;
    }
  }

}
