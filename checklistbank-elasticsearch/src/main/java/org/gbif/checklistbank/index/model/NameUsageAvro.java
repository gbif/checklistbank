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
package org.gbif.checklistbank.index.model;

import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.SchemaStore;
import org.apache.avro.specific.SpecificData;

import com.fasterxml.jackson.annotation.JsonIgnore;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class NameUsageAvro extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 3145578184246675825L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"NameUsageAvro\",\"namespace\":\"org.gbif.checklistbank.index.model\",\"fields\":[{\"name\":\"key\",\"type\":[\"int\",\"null\"]},{\"name\":\"nubKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"datasetKey\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"constituentKey\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"parentKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"parent\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"acceptedKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"accepted\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"basionymKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"basionym\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"scientificName\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"canonicalName\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"nameKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"nameType\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"authorship\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"origin\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"taxonomicStatus\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"nomenclaturalStatus\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"threatStatus\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"rank\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"rankKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"habitat\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"publishedIn\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"accordingTo\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"kingdomKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"kingdom\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"phylumKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"phylum\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"classKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"clazz\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"orderKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"order\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"familyKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"family\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"genusKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"genus\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"subgenusKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"subgenus\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"speciesKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"species\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"numDescendants\",\"type\":[\"int\",\"null\"]},{\"name\":\"sourceId\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"isSynonym\",\"type\":[\"boolean\",\"null\"]},{\"name\":\"extinct\",\"type\":[\"boolean\",\"null\"]},{\"name\":\"description\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"vernacularName\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"vernacularLang\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"vernacularNameLang\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"higherTaxonKey\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}],\"default\":null},{\"name\":\"issues\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<NameUsageAvro> ENCODER =
      new BinaryMessageEncoder<NameUsageAvro>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<NameUsageAvro> DECODER =
      new BinaryMessageDecoder<NameUsageAvro>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   */
  public static BinaryMessageDecoder<NameUsageAvro> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   */
  public static BinaryMessageDecoder<NameUsageAvro> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<NameUsageAvro>(MODEL$, SCHEMA$, resolver);
  }

  /** Serializes this NameUsageAvro to a ByteBuffer. */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /** Deserializes a NameUsageAvro from a ByteBuffer. */
  public static NameUsageAvro fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  @Deprecated public java.lang.Integer key;
  @Deprecated public java.lang.Integer nubKey;
  @Deprecated public java.lang.String datasetKey;
  @Deprecated public java.lang.String constituentKey;
  @Deprecated public java.lang.Integer parentKey;
  @Deprecated public java.lang.String parent;
  @Deprecated public java.lang.Integer acceptedKey;
  @Deprecated public java.lang.String accepted;
  @Deprecated public java.lang.Integer basionymKey;
  @Deprecated public java.lang.String basionym;
  @Deprecated public java.lang.String scientificName;
  @Deprecated public java.lang.String canonicalName;
  @Deprecated public java.lang.Integer nameKey;
  @Deprecated public java.lang.String nameType;
  @Deprecated public java.lang.String authorship;
  @Deprecated public java.lang.String origin;
  @Deprecated public java.lang.String taxonomicStatus;
  @Deprecated public java.util.List<java.lang.String> nomenclaturalStatus;
  @Deprecated public java.util.List<java.lang.String> threatStatus;
  @Deprecated public java.lang.String rank;
  @Deprecated public java.lang.Integer rankKey;
  @Deprecated public java.util.List<java.lang.String> habitat;
  @Deprecated public java.lang.String publishedIn;
  @Deprecated public java.lang.String accordingTo;
  @Deprecated public java.lang.Integer kingdomKey;
  @Deprecated public java.lang.String kingdom;
  @Deprecated public java.lang.Integer phylumKey;
  @Deprecated public java.lang.String phylum;
  @Deprecated public java.lang.Integer classKey;
  @Deprecated public java.lang.String clazz;
  @Deprecated public java.lang.Integer orderKey;
  @Deprecated public java.lang.String order;
  @Deprecated public java.lang.Integer familyKey;
  @Deprecated public java.lang.String family;
  @Deprecated public java.lang.Integer genusKey;
  @Deprecated public java.lang.String genus;
  @Deprecated public java.lang.Integer subgenusKey;
  @Deprecated public java.lang.String subgenus;
  @Deprecated public java.lang.Integer speciesKey;
  @Deprecated public java.lang.String species;
  @Deprecated public java.lang.Integer numDescendants;
  @Deprecated public java.lang.String sourceId;
  @Deprecated public java.lang.Boolean isSynonym;
  @Deprecated public java.lang.Boolean extinct;
  @Deprecated public java.util.List<java.lang.String> description;
  @Deprecated public java.util.List<java.lang.String> vernacularName;
  @Deprecated public java.util.List<java.lang.String> vernacularLang;
  @Deprecated public java.util.List<java.lang.String> vernacularNameLang;
  @Deprecated public java.util.List<java.lang.Integer> higherTaxonKey;
  @Deprecated public java.util.List<java.lang.String> issues;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public NameUsageAvro() {}

  /**
   * All-args constructor.
   * @param key The new value for key
   * @param nubKey The new value for nubKey
   * @param datasetKey The new value for datasetKey
   * @param constituentKey The new value for constituentKey
   * @param parentKey The new value for parentKey
   * @param parent The new value for parent
   * @param acceptedKey The new value for acceptedKey
   * @param accepted The new value for accepted
   * @param basionymKey The new value for basionymKey
   * @param basionym The new value for basionym
   * @param scientificName The new value for scientificName
   * @param canonicalName The new value for canonicalName
   * @param nameKey The new value for nameKey
   * @param nameType The new value for nameType
   * @param authorship The new value for authorship
   * @param origin The new value for origin
   * @param taxonomicStatus The new value for taxonomicStatus
   * @param nomenclaturalStatus The new value for nomenclaturalStatus
   * @param threatStatus The new value for threatStatus
   * @param rank The new value for rank
   * @param rankKey The new value for rankKey
   * @param habitat The new value for habitat
   * @param publishedIn The new value for publishedIn
   * @param accordingTo The new value for accordingTo
   * @param kingdomKey The new value for kingdomKey
   * @param kingdom The new value for kingdom
   * @param phylumKey The new value for phylumKey
   * @param phylum The new value for phylum
   * @param classKey The new value for classKey
   * @param clazz The new value for clazz
   * @param orderKey The new value for orderKey
   * @param order The new value for order
   * @param familyKey The new value for familyKey
   * @param family The new value for family
   * @param genusKey The new value for genusKey
   * @param genus The new value for genus
   * @param subgenusKey The new value for subgenusKey
   * @param subgenus The new value for subgenus
   * @param speciesKey The new value for speciesKey
   * @param species The new value for species
   * @param numDescendants The new value for numDescendants
   * @param sourceId The new value for sourceId
   * @param isSynonym The new value for isSynonym
   * @param extinct The new value for extinct
   * @param description The new value for description
   * @param vernacularName The new value for vernacularName
   * @param vernacularLang The new value for vernacularLang
   * @param vernacularNameLang The new value for vernacularNameLang
   * @param higherTaxonKey The new value for higherTaxonKey
   * @param issues The new value for issues
   */
  public NameUsageAvro(java.lang.Integer key, java.lang.Integer nubKey, java.lang.String datasetKey, java.lang.String constituentKey, java.lang.Integer parentKey, java.lang.String parent, java.lang.Integer acceptedKey, java.lang.String accepted, java.lang.Integer basionymKey, java.lang.String basionym, java.lang.String scientificName, java.lang.String canonicalName, java.lang.Integer nameKey, java.lang.String nameType, java.lang.String authorship, java.lang.String origin, java.lang.String taxonomicStatus, java.util.List<java.lang.String> nomenclaturalStatus, java.util.List<java.lang.String> threatStatus, java.lang.String rank, java.lang.Integer rankKey, java.util.List<java.lang.String> habitat, java.lang.String publishedIn, java.lang.String accordingTo, java.lang.Integer kingdomKey, java.lang.String kingdom, java.lang.Integer phylumKey, java.lang.String phylum, java.lang.Integer classKey, java.lang.String clazz, java.lang.Integer orderKey, java.lang.String order, java.lang.Integer familyKey, java.lang.String family, java.lang.Integer genusKey, java.lang.String genus, java.lang.Integer subgenusKey, java.lang.String subgenus, java.lang.Integer speciesKey, java.lang.String species, java.lang.Integer numDescendants, java.lang.String sourceId, java.lang.Boolean isSynonym, java.lang.Boolean extinct, java.util.List<java.lang.String> description, java.util.List<java.lang.String> vernacularName, java.util.List<java.lang.String> vernacularLang, java.util.List<java.lang.String> vernacularNameLang, java.util.List<java.lang.Integer> higherTaxonKey, java.util.List<java.lang.String> issues) {
    this.key = key;
    this.nubKey = nubKey;
    this.datasetKey = datasetKey;
    this.constituentKey = constituentKey;
    this.parentKey = parentKey;
    this.parent = parent;
    this.acceptedKey = acceptedKey;
    this.accepted = accepted;
    this.basionymKey = basionymKey;
    this.basionym = basionym;
    this.scientificName = scientificName;
    this.canonicalName = canonicalName;
    this.nameKey = nameKey;
    this.nameType = nameType;
    this.authorship = authorship;
    this.origin = origin;
    this.taxonomicStatus = taxonomicStatus;
    this.nomenclaturalStatus = nomenclaturalStatus;
    this.threatStatus = threatStatus;
    this.rank = rank;
    this.rankKey = rankKey;
    this.habitat = habitat;
    this.publishedIn = publishedIn;
    this.accordingTo = accordingTo;
    this.kingdomKey = kingdomKey;
    this.kingdom = kingdom;
    this.phylumKey = phylumKey;
    this.phylum = phylum;
    this.classKey = classKey;
    this.clazz = clazz;
    this.orderKey = orderKey;
    this.order = order;
    this.familyKey = familyKey;
    this.family = family;
    this.genusKey = genusKey;
    this.genus = genus;
    this.subgenusKey = subgenusKey;
    this.subgenus = subgenus;
    this.speciesKey = speciesKey;
    this.species = species;
    this.numDescendants = numDescendants;
    this.sourceId = sourceId;
    this.isSynonym = isSynonym;
    this.extinct = extinct;
    this.description = description;
    this.vernacularName = vernacularName;
    this.vernacularLang = vernacularLang;
    this.vernacularNameLang = vernacularNameLang;
    this.higherTaxonKey = higherTaxonKey;
    this.issues = issues;
  }

  @Override
  @JsonIgnore
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return key;
    case 1: return nubKey;
    case 2: return datasetKey;
    case 3: return constituentKey;
    case 4: return parentKey;
    case 5: return parent;
    case 6: return acceptedKey;
    case 7: return accepted;
    case 8: return basionymKey;
    case 9: return basionym;
    case 10: return scientificName;
    case 11: return canonicalName;
    case 12: return nameKey;
    case 13: return nameType;
    case 14: return authorship;
    case 15: return origin;
    case 16: return taxonomicStatus;
    case 17: return nomenclaturalStatus;
    case 18: return threatStatus;
    case 19: return rank;
    case 20: return rankKey;
    case 21: return habitat;
    case 22: return publishedIn;
    case 23: return accordingTo;
    case 24: return kingdomKey;
    case 25: return kingdom;
    case 26: return phylumKey;
    case 27: return phylum;
    case 28: return classKey;
    case 29: return clazz;
    case 30: return orderKey;
    case 31: return order;
    case 32: return familyKey;
    case 33: return family;
    case 34: return genusKey;
    case 35: return genus;
    case 36: return subgenusKey;
    case 37: return subgenus;
    case 38: return speciesKey;
    case 39: return species;
    case 40: return numDescendants;
    case 41: return sourceId;
    case 42: return isSynonym;
    case 43: return extinct;
    case 44: return description;
    case 45: return vernacularName;
    case 46: return vernacularLang;
    case 47: return vernacularNameLang;
    case 48: return higherTaxonKey;
    case 49: return issues;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  @Override
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: key = (java.lang.Integer)value$; break;
    case 1: nubKey = (java.lang.Integer)value$; break;
    case 2: datasetKey = (java.lang.String)value$; break;
    case 3: constituentKey = (java.lang.String)value$; break;
    case 4: parentKey = (java.lang.Integer)value$; break;
    case 5: parent = (java.lang.String)value$; break;
    case 6: acceptedKey = (java.lang.Integer)value$; break;
    case 7: accepted = (java.lang.String)value$; break;
    case 8: basionymKey = (java.lang.Integer)value$; break;
    case 9: basionym = (java.lang.String)value$; break;
    case 10: scientificName = (java.lang.String)value$; break;
    case 11: canonicalName = (java.lang.String)value$; break;
    case 12: nameKey = (java.lang.Integer)value$; break;
    case 13: nameType = (java.lang.String)value$; break;
    case 14: authorship = (java.lang.String)value$; break;
    case 15: origin = (java.lang.String)value$; break;
    case 16: taxonomicStatus = (java.lang.String)value$; break;
    case 17: nomenclaturalStatus = (java.util.List<java.lang.String>)value$; break;
    case 18: threatStatus = (java.util.List<java.lang.String>)value$; break;
    case 19: rank = (java.lang.String)value$; break;
    case 20: rankKey = (java.lang.Integer)value$; break;
    case 21: habitat = (java.util.List<java.lang.String>)value$; break;
    case 22: publishedIn = (java.lang.String)value$; break;
    case 23: accordingTo = (java.lang.String)value$; break;
    case 24: kingdomKey = (java.lang.Integer)value$; break;
    case 25: kingdom = (java.lang.String)value$; break;
    case 26: phylumKey = (java.lang.Integer)value$; break;
    case 27: phylum = (java.lang.String)value$; break;
    case 28: classKey = (java.lang.Integer)value$; break;
    case 29: clazz = (java.lang.String)value$; break;
    case 30: orderKey = (java.lang.Integer)value$; break;
    case 31: order = (java.lang.String)value$; break;
    case 32: familyKey = (java.lang.Integer)value$; break;
    case 33: family = (java.lang.String)value$; break;
    case 34: genusKey = (java.lang.Integer)value$; break;
    case 35: genus = (java.lang.String)value$; break;
    case 36: subgenusKey = (java.lang.Integer)value$; break;
    case 37: subgenus = (java.lang.String)value$; break;
    case 38: speciesKey = (java.lang.Integer)value$; break;
    case 39: species = (java.lang.String)value$; break;
    case 40: numDescendants = (java.lang.Integer)value$; break;
    case 41: sourceId = (java.lang.String)value$; break;
    case 42: isSynonym = (java.lang.Boolean)value$; break;
    case 43: extinct = (java.lang.Boolean)value$; break;
    case 44: description = (java.util.List<java.lang.String>)value$; break;
    case 45: vernacularName = (java.util.List<java.lang.String>)value$; break;
    case 46: vernacularLang = (java.util.List<java.lang.String>)value$; break;
    case 47: vernacularNameLang = (java.util.List<java.lang.String>)value$; break;
    case 48: higherTaxonKey = (java.util.List<java.lang.Integer>)value$; break;
    case 49: issues = (java.util.List<java.lang.String>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'key' field.
   * @return The value of the 'key' field.
   */
  public java.lang.Integer getKey() {
    return key;
  }

  /**
   * Sets the value of the 'key' field.
   * @param value the value to set.
   */
  public void setKey(java.lang.Integer value) {
    this.key = value;
  }

  /**
   * Gets the value of the 'nubKey' field.
   * @return The value of the 'nubKey' field.
   */
  public java.lang.Integer getNubKey() {
    return nubKey;
  }

  /**
   * Sets the value of the 'nubKey' field.
   * @param value the value to set.
   */
  public void setNubKey(java.lang.Integer value) {
    this.nubKey = value;
  }

  /**
   * Gets the value of the 'datasetKey' field.
   * @return The value of the 'datasetKey' field.
   */
  public java.lang.String getDatasetKey() {
    return datasetKey;
  }

  /**
   * Sets the value of the 'datasetKey' field.
   * @param value the value to set.
   */
  public void setDatasetKey(java.lang.String value) {
    this.datasetKey = value;
  }

  /**
   * Gets the value of the 'constituentKey' field.
   * @return The value of the 'constituentKey' field.
   */
  public java.lang.String getConstituentKey() {
    return constituentKey;
  }

  /**
   * Sets the value of the 'constituentKey' field.
   * @param value the value to set.
   */
  public void setConstituentKey(java.lang.String value) {
    this.constituentKey = value;
  }

  /**
   * Gets the value of the 'parentKey' field.
   * @return The value of the 'parentKey' field.
   */
  public java.lang.Integer getParentKey() {
    return parentKey;
  }

  /**
   * Sets the value of the 'parentKey' field.
   * @param value the value to set.
   */
  public void setParentKey(java.lang.Integer value) {
    this.parentKey = value;
  }

  /**
   * Gets the value of the 'parent' field.
   * @return The value of the 'parent' field.
   */
  public java.lang.String getParent() {
    return parent;
  }

  /**
   * Sets the value of the 'parent' field.
   * @param value the value to set.
   */
  public void setParent(java.lang.String value) {
    this.parent = value;
  }

  /**
   * Gets the value of the 'acceptedKey' field.
   * @return The value of the 'acceptedKey' field.
   */
  public java.lang.Integer getAcceptedKey() {
    return acceptedKey;
  }

  /**
   * Sets the value of the 'acceptedKey' field.
   * @param value the value to set.
   */
  public void setAcceptedKey(java.lang.Integer value) {
    this.acceptedKey = value;
  }

  /**
   * Gets the value of the 'accepted' field.
   * @return The value of the 'accepted' field.
   */
  public java.lang.String getAccepted() {
    return accepted;
  }

  /**
   * Sets the value of the 'accepted' field.
   * @param value the value to set.
   */
  public void setAccepted(java.lang.String value) {
    this.accepted = value;
  }

  /**
   * Gets the value of the 'basionymKey' field.
   * @return The value of the 'basionymKey' field.
   */
  public java.lang.Integer getBasionymKey() {
    return basionymKey;
  }

  /**
   * Sets the value of the 'basionymKey' field.
   * @param value the value to set.
   */
  public void setBasionymKey(java.lang.Integer value) {
    this.basionymKey = value;
  }

  /**
   * Gets the value of the 'basionym' field.
   * @return The value of the 'basionym' field.
   */
  public java.lang.String getBasionym() {
    return basionym;
  }

  /**
   * Sets the value of the 'basionym' field.
   * @param value the value to set.
   */
  public void setBasionym(java.lang.String value) {
    this.basionym = value;
  }

  /**
   * Gets the value of the 'scientificName' field.
   * @return The value of the 'scientificName' field.
   */
  public java.lang.String getScientificName() {
    return scientificName;
  }

  /**
   * Sets the value of the 'scientificName' field.
   * @param value the value to set.
   */
  public void setScientificName(java.lang.String value) {
    this.scientificName = value;
  }

  /**
   * Gets the value of the 'canonicalName' field.
   * @return The value of the 'canonicalName' field.
   */
  public java.lang.String getCanonicalName() {
    return canonicalName;
  }

  /**
   * Sets the value of the 'canonicalName' field.
   * @param value the value to set.
   */
  public void setCanonicalName(java.lang.String value) {
    this.canonicalName = value;
  }

  /**
   * Gets the value of the 'nameKey' field.
   * @return The value of the 'nameKey' field.
   */
  public java.lang.Integer getNameKey() {
    return nameKey;
  }

  /**
   * Sets the value of the 'nameKey' field.
   * @param value the value to set.
   */
  public void setNameKey(java.lang.Integer value) {
    this.nameKey = value;
  }

  /**
   * Gets the value of the 'nameType' field.
   * @return The value of the 'nameType' field.
   */
  public java.lang.String getNameType() {
    return nameType;
  }

  /**
   * Sets the value of the 'nameType' field.
   * @param value the value to set.
   */
  public void setNameType(java.lang.String value) {
    this.nameType = value;
  }

  /**
   * Gets the value of the 'authorship' field.
   * @return The value of the 'authorship' field.
   */
  public java.lang.String getAuthorship() {
    return authorship;
  }

  /**
   * Sets the value of the 'authorship' field.
   * @param value the value to set.
   */
  public void setAuthorship(java.lang.String value) {
    this.authorship = value;
  }

  /**
   * Gets the value of the 'origin' field.
   * @return The value of the 'origin' field.
   */
  public java.lang.String getOrigin() {
    return origin;
  }

  /**
   * Sets the value of the 'origin' field.
   * @param value the value to set.
   */
  public void setOrigin(java.lang.String value) {
    this.origin = value;
  }

  /**
   * Gets the value of the 'taxonomicStatus' field.
   * @return The value of the 'taxonomicStatus' field.
   */
  public java.lang.String getTaxonomicStatus() {
    return taxonomicStatus;
  }

  /**
   * Sets the value of the 'taxonomicStatus' field.
   * @param value the value to set.
   */
  public void setTaxonomicStatus(java.lang.String value) {
    this.taxonomicStatus = value;
  }

  /**
   * Gets the value of the 'nomenclaturalStatus' field.
   * @return The value of the 'nomenclaturalStatus' field.
   */
  public java.util.List<java.lang.String> getNomenclaturalStatus() {
    return nomenclaturalStatus;
  }

  /**
   * Sets the value of the 'nomenclaturalStatus' field.
   * @param value the value to set.
   */
  public void setNomenclaturalStatus(java.util.List<java.lang.String> value) {
    this.nomenclaturalStatus = value;
  }

  /**
   * Gets the value of the 'threatStatus' field.
   * @return The value of the 'threatStatus' field.
   */
  public java.util.List<java.lang.String> getThreatStatus() {
    return threatStatus;
  }

  /**
   * Sets the value of the 'threatStatus' field.
   * @param value the value to set.
   */
  public void setThreatStatus(java.util.List<java.lang.String> value) {
    this.threatStatus = value;
  }

  /**
   * Gets the value of the 'rank' field.
   * @return The value of the 'rank' field.
   */
  public java.lang.String getRank() {
    return rank;
  }

  /**
   * Sets the value of the 'rank' field.
   * @param value the value to set.
   */
  public void setRank(java.lang.String value) {
    this.rank = value;
  }

  /**
   * Gets the value of the 'rankKey' field.
   * @return The value of the 'rankKey' field.
   */
  public java.lang.Integer getRankKey() {
    return rankKey;
  }

  /**
   * Sets the value of the 'rankKey' field.
   * @param value the value to set.
   */
  public void setRankKey(java.lang.Integer value) {
    this.rankKey = value;
  }

  /**
   * Gets the value of the 'habitat' field.
   * @return The value of the 'habitat' field.
   */
  public java.util.List<java.lang.String> getHabitat() {
    return habitat;
  }

  /**
   * Sets the value of the 'habitat' field.
   * @param value the value to set.
   */
  public void setHabitat(java.util.List<java.lang.String> value) {
    this.habitat = value;
  }

  /**
   * Gets the value of the 'publishedIn' field.
   * @return The value of the 'publishedIn' field.
   */
  public java.lang.String getPublishedIn() {
    return publishedIn;
  }

  /**
   * Sets the value of the 'publishedIn' field.
   * @param value the value to set.
   */
  public void setPublishedIn(java.lang.String value) {
    this.publishedIn = value;
  }

  /**
   * Gets the value of the 'accordingTo' field.
   * @return The value of the 'accordingTo' field.
   */
  public java.lang.String getAccordingTo() {
    return accordingTo;
  }

  /**
   * Sets the value of the 'accordingTo' field.
   * @param value the value to set.
   */
  public void setAccordingTo(java.lang.String value) {
    this.accordingTo = value;
  }

  /**
   * Gets the value of the 'kingdomKey' field.
   * @return The value of the 'kingdomKey' field.
   */
  public java.lang.Integer getKingdomKey() {
    return kingdomKey;
  }

  /**
   * Sets the value of the 'kingdomKey' field.
   * @param value the value to set.
   */
  public void setKingdomKey(java.lang.Integer value) {
    this.kingdomKey = value;
  }

  /**
   * Gets the value of the 'kingdom' field.
   * @return The value of the 'kingdom' field.
   */
  public java.lang.String getKingdom() {
    return kingdom;
  }

  /**
   * Sets the value of the 'kingdom' field.
   * @param value the value to set.
   */
  public void setKingdom(java.lang.String value) {
    this.kingdom = value;
  }

  /**
   * Gets the value of the 'phylumKey' field.
   * @return The value of the 'phylumKey' field.
   */
  public java.lang.Integer getPhylumKey() {
    return phylumKey;
  }

  /**
   * Sets the value of the 'phylumKey' field.
   * @param value the value to set.
   */
  public void setPhylumKey(java.lang.Integer value) {
    this.phylumKey = value;
  }

  /**
   * Gets the value of the 'phylum' field.
   * @return The value of the 'phylum' field.
   */
  public java.lang.String getPhylum() {
    return phylum;
  }

  /**
   * Sets the value of the 'phylum' field.
   * @param value the value to set.
   */
  public void setPhylum(java.lang.String value) {
    this.phylum = value;
  }

  /**
   * Gets the value of the 'classKey' field.
   * @return The value of the 'classKey' field.
   */
  public java.lang.Integer getClassKey() {
    return classKey;
  }

  /**
   * Sets the value of the 'classKey' field.
   * @param value the value to set.
   */
  public void setClassKey(java.lang.Integer value) {
    this.classKey = value;
  }

  /**
   * Gets the value of the 'clazz' field.
   * @return The value of the 'clazz' field.
   */
  public java.lang.String getClazz() {
    return clazz;
  }

  /**
   * Sets the value of the 'clazz' field.
   * @param value the value to set.
   */
  public void setClazz(java.lang.String value) {
    this.clazz = value;
  }

  /**
   * Gets the value of the 'orderKey' field.
   * @return The value of the 'orderKey' field.
   */
  public java.lang.Integer getOrderKey() {
    return orderKey;
  }

  /**
   * Sets the value of the 'orderKey' field.
   * @param value the value to set.
   */
  public void setOrderKey(java.lang.Integer value) {
    this.orderKey = value;
  }

  /**
   * Gets the value of the 'order' field.
   * @return The value of the 'order' field.
   */
  public java.lang.String getOrder() {
    return order;
  }

  /**
   * Sets the value of the 'order' field.
   * @param value the value to set.
   */
  public void setOrder(java.lang.String value) {
    this.order = value;
  }

  /**
   * Gets the value of the 'familyKey' field.
   * @return The value of the 'familyKey' field.
   */
  public java.lang.Integer getFamilyKey() {
    return familyKey;
  }

  /**
   * Sets the value of the 'familyKey' field.
   * @param value the value to set.
   */
  public void setFamilyKey(java.lang.Integer value) {
    this.familyKey = value;
  }

  /**
   * Gets the value of the 'family' field.
   * @return The value of the 'family' field.
   */
  public java.lang.String getFamily() {
    return family;
  }

  /**
   * Sets the value of the 'family' field.
   * @param value the value to set.
   */
  public void setFamily(java.lang.String value) {
    this.family = value;
  }

  /**
   * Gets the value of the 'genusKey' field.
   * @return The value of the 'genusKey' field.
   */
  public java.lang.Integer getGenusKey() {
    return genusKey;
  }

  /**
   * Sets the value of the 'genusKey' field.
   * @param value the value to set.
   */
  public void setGenusKey(java.lang.Integer value) {
    this.genusKey = value;
  }

  /**
   * Gets the value of the 'genus' field.
   * @return The value of the 'genus' field.
   */
  public java.lang.String getGenus() {
    return genus;
  }

  /**
   * Sets the value of the 'genus' field.
   * @param value the value to set.
   */
  public void setGenus(java.lang.String value) {
    this.genus = value;
  }

  /**
   * Gets the value of the 'subgenusKey' field.
   * @return The value of the 'subgenusKey' field.
   */
  public java.lang.Integer getSubgenusKey() {
    return subgenusKey;
  }

  /**
   * Sets the value of the 'subgenusKey' field.
   * @param value the value to set.
   */
  public void setSubgenusKey(java.lang.Integer value) {
    this.subgenusKey = value;
  }

  /**
   * Gets the value of the 'subgenus' field.
   * @return The value of the 'subgenus' field.
   */
  public java.lang.String getSubgenus() {
    return subgenus;
  }

  /**
   * Sets the value of the 'subgenus' field.
   * @param value the value to set.
   */
  public void setSubgenus(java.lang.String value) {
    this.subgenus = value;
  }

  /**
   * Gets the value of the 'speciesKey' field.
   * @return The value of the 'speciesKey' field.
   */
  public java.lang.Integer getSpeciesKey() {
    return speciesKey;
  }

  /**
   * Sets the value of the 'speciesKey' field.
   * @param value the value to set.
   */
  public void setSpeciesKey(java.lang.Integer value) {
    this.speciesKey = value;
  }

  /**
   * Gets the value of the 'species' field.
   * @return The value of the 'species' field.
   */
  public java.lang.String getSpecies() {
    return species;
  }

  /**
   * Sets the value of the 'species' field.
   * @param value the value to set.
   */
  public void setSpecies(java.lang.String value) {
    this.species = value;
  }

  /**
   * Gets the value of the 'numDescendants' field.
   * @return The value of the 'numDescendants' field.
   */
  public java.lang.Integer getNumDescendants() {
    return numDescendants;
  }

  /**
   * Sets the value of the 'numDescendants' field.
   * @param value the value to set.
   */
  public void setNumDescendants(java.lang.Integer value) {
    this.numDescendants = value;
  }

  /**
   * Gets the value of the 'sourceId' field.
   * @return The value of the 'sourceId' field.
   */
  public java.lang.String getSourceId() {
    return sourceId;
  }

  /**
   * Sets the value of the 'sourceId' field.
   * @param value the value to set.
   */
  public void setSourceId(java.lang.String value) {
    this.sourceId = value;
  }

  /**
   * Gets the value of the 'isSynonym' field.
   * @return The value of the 'isSynonym' field.
   */
  public java.lang.Boolean getIsSynonym() {
    return isSynonym;
  }

  /**
   * Sets the value of the 'isSynonym' field.
   * @param value the value to set.
   */
  public void setIsSynonym(java.lang.Boolean value) {
    this.isSynonym = value;
  }

  /**
   * Gets the value of the 'extinct' field.
   * @return The value of the 'extinct' field.
   */
  public java.lang.Boolean getExtinct() {
    return extinct;
  }

  /**
   * Sets the value of the 'extinct' field.
   * @param value the value to set.
   */
  public void setExtinct(java.lang.Boolean value) {
    this.extinct = value;
  }

  /**
   * Gets the value of the 'description' field.
   * @return The value of the 'description' field.
   */
  public java.util.List<java.lang.String> getDescription() {
    return description;
  }

  /**
   * Sets the value of the 'description' field.
   * @param value the value to set.
   */
  public void setDescription(java.util.List<java.lang.String> value) {
    this.description = value;
  }

  /**
   * Gets the value of the 'vernacularName' field.
   * @return The value of the 'vernacularName' field.
   */
  public java.util.List<java.lang.String> getVernacularName() {
    return vernacularName;
  }

  /**
   * Sets the value of the 'vernacularName' field.
   * @param value the value to set.
   */
  public void setVernacularName(java.util.List<java.lang.String> value) {
    this.vernacularName = value;
  }

  /**
   * Gets the value of the 'vernacularLang' field.
   * @return The value of the 'vernacularLang' field.
   */
  public java.util.List<java.lang.String> getVernacularLang() {
    return vernacularLang;
  }

  /**
   * Sets the value of the 'vernacularLang' field.
   * @param value the value to set.
   */
  public void setVernacularLang(java.util.List<java.lang.String> value) {
    this.vernacularLang = value;
  }

  /**
   * Gets the value of the 'vernacularNameLang' field.
   * @return The value of the 'vernacularNameLang' field.
   */
  public java.util.List<java.lang.String> getVernacularNameLang() {
    return vernacularNameLang;
  }

  /**
   * Sets the value of the 'vernacularNameLang' field.
   * @param value the value to set.
   */
  public void setVernacularNameLang(java.util.List<java.lang.String> value) {
    this.vernacularNameLang = value;
  }

  /**
   * Gets the value of the 'higherTaxonKey' field.
   * @return The value of the 'higherTaxonKey' field.
   */
  public java.util.List<java.lang.Integer> getHigherTaxonKey() {
    return higherTaxonKey;
  }

  /**
   * Sets the value of the 'higherTaxonKey' field.
   * @param value the value to set.
   */
  public void setHigherTaxonKey(java.util.List<java.lang.Integer> value) {
    this.higherTaxonKey = value;
  }

  /**
   * Gets the value of the 'issues' field.
   * @return The value of the 'issues' field.
   */
  public java.util.List<java.lang.String> getIssues() {
    return issues;
  }

  /**
   * Sets the value of the 'issues' field.
   * @param value the value to set.
   */
  public void setIssues(java.util.List<java.lang.String> value) {
    this.issues = value;
  }

  /**
   * Creates a new NameUsageAvro RecordBuilder.
   * @return A new NameUsageAvro RecordBuilder
   */
  public static org.gbif.checklistbank.index.model.NameUsageAvro.Builder newBuilder() {
    return new org.gbif.checklistbank.index.model.NameUsageAvro.Builder();
  }

  /**
   * Creates a new NameUsageAvro RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new NameUsageAvro RecordBuilder
   */
  public static org.gbif.checklistbank.index.model.NameUsageAvro.Builder newBuilder(org.gbif.checklistbank.index.model.NameUsageAvro.Builder other) {
    return new org.gbif.checklistbank.index.model.NameUsageAvro.Builder(other);
  }

  /**
   * Creates a new NameUsageAvro RecordBuilder by copying an existing NameUsageAvro instance.
   * @param other The existing instance to copy.
   * @return A new NameUsageAvro RecordBuilder
   */
  public static org.gbif.checklistbank.index.model.NameUsageAvro.Builder newBuilder(org.gbif.checklistbank.index.model.NameUsageAvro other) {
    return new org.gbif.checklistbank.index.model.NameUsageAvro.Builder(other);
  }

  /**
   * RecordBuilder for NameUsageAvro instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<NameUsageAvro>
    implements org.apache.avro.data.RecordBuilder<NameUsageAvro> {

    private java.lang.Integer key;
    private java.lang.Integer nubKey;
    private java.lang.String datasetKey;
    private java.lang.String constituentKey;
    private java.lang.Integer parentKey;
    private java.lang.String parent;
    private java.lang.Integer acceptedKey;
    private java.lang.String accepted;
    private java.lang.Integer basionymKey;
    private java.lang.String basionym;
    private java.lang.String scientificName;
    private java.lang.String canonicalName;
    private java.lang.Integer nameKey;
    private java.lang.String nameType;
    private java.lang.String authorship;
    private java.lang.String origin;
    private java.lang.String taxonomicStatus;
    private java.util.List<java.lang.String> nomenclaturalStatus;
    private java.util.List<java.lang.String> threatStatus;
    private java.lang.String rank;
    private java.lang.Integer rankKey;
    private java.util.List<java.lang.String> habitat;
    private java.lang.String publishedIn;
    private java.lang.String accordingTo;
    private java.lang.Integer kingdomKey;
    private java.lang.String kingdom;
    private java.lang.Integer phylumKey;
    private java.lang.String phylum;
    private java.lang.Integer classKey;
    private java.lang.String clazz;
    private java.lang.Integer orderKey;
    private java.lang.String order;
    private java.lang.Integer familyKey;
    private java.lang.String family;
    private java.lang.Integer genusKey;
    private java.lang.String genus;
    private java.lang.Integer subgenusKey;
    private java.lang.String subgenus;
    private java.lang.Integer speciesKey;
    private java.lang.String species;
    private java.lang.Integer numDescendants;
    private java.lang.String sourceId;
    private java.lang.Boolean isSynonym;
    private java.lang.Boolean extinct;
    private java.util.List<java.lang.String> description;
    private java.util.List<java.lang.String> vernacularName;
    private java.util.List<java.lang.String> vernacularLang;
    private java.util.List<java.lang.String> vernacularNameLang;
    private java.util.List<java.lang.Integer> higherTaxonKey;
    private java.util.List<java.lang.String> issues;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(org.gbif.checklistbank.index.model.NameUsageAvro.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.key)) {
        this.key = data().deepCopy(fields()[0].schema(), other.key);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.nubKey)) {
        this.nubKey = data().deepCopy(fields()[1].schema(), other.nubKey);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.datasetKey)) {
        this.datasetKey = data().deepCopy(fields()[2].schema(), other.datasetKey);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.constituentKey)) {
        this.constituentKey = data().deepCopy(fields()[3].schema(), other.constituentKey);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.parentKey)) {
        this.parentKey = data().deepCopy(fields()[4].schema(), other.parentKey);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.parent)) {
        this.parent = data().deepCopy(fields()[5].schema(), other.parent);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.acceptedKey)) {
        this.acceptedKey = data().deepCopy(fields()[6].schema(), other.acceptedKey);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.accepted)) {
        this.accepted = data().deepCopy(fields()[7].schema(), other.accepted);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.basionymKey)) {
        this.basionymKey = data().deepCopy(fields()[8].schema(), other.basionymKey);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.basionym)) {
        this.basionym = data().deepCopy(fields()[9].schema(), other.basionym);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.scientificName)) {
        this.scientificName = data().deepCopy(fields()[10].schema(), other.scientificName);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.canonicalName)) {
        this.canonicalName = data().deepCopy(fields()[11].schema(), other.canonicalName);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.nameKey)) {
        this.nameKey = data().deepCopy(fields()[12].schema(), other.nameKey);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.nameType)) {
        this.nameType = data().deepCopy(fields()[13].schema(), other.nameType);
        fieldSetFlags()[13] = true;
      }
      if (isValidValue(fields()[14], other.authorship)) {
        this.authorship = data().deepCopy(fields()[14].schema(), other.authorship);
        fieldSetFlags()[14] = true;
      }
      if (isValidValue(fields()[15], other.origin)) {
        this.origin = data().deepCopy(fields()[15].schema(), other.origin);
        fieldSetFlags()[15] = true;
      }
      if (isValidValue(fields()[16], other.taxonomicStatus)) {
        this.taxonomicStatus = data().deepCopy(fields()[16].schema(), other.taxonomicStatus);
        fieldSetFlags()[16] = true;
      }
      if (isValidValue(fields()[17], other.nomenclaturalStatus)) {
        this.nomenclaturalStatus = data().deepCopy(fields()[17].schema(), other.nomenclaturalStatus);
        fieldSetFlags()[17] = true;
      }
      if (isValidValue(fields()[18], other.threatStatus)) {
        this.threatStatus = data().deepCopy(fields()[18].schema(), other.threatStatus);
        fieldSetFlags()[18] = true;
      }
      if (isValidValue(fields()[19], other.rank)) {
        this.rank = data().deepCopy(fields()[19].schema(), other.rank);
        fieldSetFlags()[19] = true;
      }
      if (isValidValue(fields()[20], other.rankKey)) {
        this.rankKey = data().deepCopy(fields()[20].schema(), other.rankKey);
        fieldSetFlags()[20] = true;
      }
      if (isValidValue(fields()[21], other.habitat)) {
        this.habitat = data().deepCopy(fields()[21].schema(), other.habitat);
        fieldSetFlags()[21] = true;
      }
      if (isValidValue(fields()[22], other.publishedIn)) {
        this.publishedIn = data().deepCopy(fields()[22].schema(), other.publishedIn);
        fieldSetFlags()[22] = true;
      }
      if (isValidValue(fields()[23], other.accordingTo)) {
        this.accordingTo = data().deepCopy(fields()[23].schema(), other.accordingTo);
        fieldSetFlags()[23] = true;
      }
      if (isValidValue(fields()[24], other.kingdomKey)) {
        this.kingdomKey = data().deepCopy(fields()[24].schema(), other.kingdomKey);
        fieldSetFlags()[24] = true;
      }
      if (isValidValue(fields()[25], other.kingdom)) {
        this.kingdom = data().deepCopy(fields()[25].schema(), other.kingdom);
        fieldSetFlags()[25] = true;
      }
      if (isValidValue(fields()[26], other.phylumKey)) {
        this.phylumKey = data().deepCopy(fields()[26].schema(), other.phylumKey);
        fieldSetFlags()[26] = true;
      }
      if (isValidValue(fields()[27], other.phylum)) {
        this.phylum = data().deepCopy(fields()[27].schema(), other.phylum);
        fieldSetFlags()[27] = true;
      }
      if (isValidValue(fields()[28], other.classKey)) {
        this.classKey = data().deepCopy(fields()[28].schema(), other.classKey);
        fieldSetFlags()[28] = true;
      }
      if (isValidValue(fields()[29], other.clazz)) {
        this.clazz = data().deepCopy(fields()[29].schema(), other.clazz);
        fieldSetFlags()[29] = true;
      }
      if (isValidValue(fields()[30], other.orderKey)) {
        this.orderKey = data().deepCopy(fields()[30].schema(), other.orderKey);
        fieldSetFlags()[30] = true;
      }
      if (isValidValue(fields()[31], other.order)) {
        this.order = data().deepCopy(fields()[31].schema(), other.order);
        fieldSetFlags()[31] = true;
      }
      if (isValidValue(fields()[32], other.familyKey)) {
        this.familyKey = data().deepCopy(fields()[32].schema(), other.familyKey);
        fieldSetFlags()[32] = true;
      }
      if (isValidValue(fields()[33], other.family)) {
        this.family = data().deepCopy(fields()[33].schema(), other.family);
        fieldSetFlags()[33] = true;
      }
      if (isValidValue(fields()[34], other.genusKey)) {
        this.genusKey = data().deepCopy(fields()[34].schema(), other.genusKey);
        fieldSetFlags()[34] = true;
      }
      if (isValidValue(fields()[35], other.genus)) {
        this.genus = data().deepCopy(fields()[35].schema(), other.genus);
        fieldSetFlags()[35] = true;
      }
      if (isValidValue(fields()[36], other.subgenusKey)) {
        this.subgenusKey = data().deepCopy(fields()[36].schema(), other.subgenusKey);
        fieldSetFlags()[36] = true;
      }
      if (isValidValue(fields()[37], other.subgenus)) {
        this.subgenus = data().deepCopy(fields()[37].schema(), other.subgenus);
        fieldSetFlags()[37] = true;
      }
      if (isValidValue(fields()[38], other.speciesKey)) {
        this.speciesKey = data().deepCopy(fields()[38].schema(), other.speciesKey);
        fieldSetFlags()[38] = true;
      }
      if (isValidValue(fields()[39], other.species)) {
        this.species = data().deepCopy(fields()[39].schema(), other.species);
        fieldSetFlags()[39] = true;
      }
      if (isValidValue(fields()[40], other.numDescendants)) {
        this.numDescendants = data().deepCopy(fields()[40].schema(), other.numDescendants);
        fieldSetFlags()[40] = true;
      }
      if (isValidValue(fields()[41], other.sourceId)) {
        this.sourceId = data().deepCopy(fields()[41].schema(), other.sourceId);
        fieldSetFlags()[41] = true;
      }
      if (isValidValue(fields()[42], other.isSynonym)) {
        this.isSynonym = data().deepCopy(fields()[42].schema(), other.isSynonym);
        fieldSetFlags()[42] = true;
      }
      if (isValidValue(fields()[43], other.extinct)) {
        this.extinct = data().deepCopy(fields()[43].schema(), other.extinct);
        fieldSetFlags()[43] = true;
      }
      if (isValidValue(fields()[44], other.description)) {
        this.description = data().deepCopy(fields()[44].schema(), other.description);
        fieldSetFlags()[44] = true;
      }
      if (isValidValue(fields()[45], other.vernacularName)) {
        this.vernacularName = data().deepCopy(fields()[45].schema(), other.vernacularName);
        fieldSetFlags()[45] = true;
      }
      if (isValidValue(fields()[46], other.vernacularLang)) {
        this.vernacularLang = data().deepCopy(fields()[46].schema(), other.vernacularLang);
        fieldSetFlags()[46] = true;
      }
      if (isValidValue(fields()[47], other.vernacularNameLang)) {
        this.vernacularNameLang = data().deepCopy(fields()[47].schema(), other.vernacularNameLang);
        fieldSetFlags()[47] = true;
      }
      if (isValidValue(fields()[48], other.higherTaxonKey)) {
        this.higherTaxonKey = data().deepCopy(fields()[48].schema(), other.higherTaxonKey);
        fieldSetFlags()[48] = true;
      }
      if (isValidValue(fields()[49], other.issues)) {
        this.issues = data().deepCopy(fields()[49].schema(), other.issues);
        fieldSetFlags()[49] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing NameUsageAvro instance
     * @param other The existing instance to copy.
     */
    private Builder(org.gbif.checklistbank.index.model.NameUsageAvro other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.key)) {
        this.key = data().deepCopy(fields()[0].schema(), other.key);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.nubKey)) {
        this.nubKey = data().deepCopy(fields()[1].schema(), other.nubKey);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.datasetKey)) {
        this.datasetKey = data().deepCopy(fields()[2].schema(), other.datasetKey);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.constituentKey)) {
        this.constituentKey = data().deepCopy(fields()[3].schema(), other.constituentKey);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.parentKey)) {
        this.parentKey = data().deepCopy(fields()[4].schema(), other.parentKey);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.parent)) {
        this.parent = data().deepCopy(fields()[5].schema(), other.parent);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.acceptedKey)) {
        this.acceptedKey = data().deepCopy(fields()[6].schema(), other.acceptedKey);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.accepted)) {
        this.accepted = data().deepCopy(fields()[7].schema(), other.accepted);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.basionymKey)) {
        this.basionymKey = data().deepCopy(fields()[8].schema(), other.basionymKey);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.basionym)) {
        this.basionym = data().deepCopy(fields()[9].schema(), other.basionym);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.scientificName)) {
        this.scientificName = data().deepCopy(fields()[10].schema(), other.scientificName);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.canonicalName)) {
        this.canonicalName = data().deepCopy(fields()[11].schema(), other.canonicalName);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.nameKey)) {
        this.nameKey = data().deepCopy(fields()[12].schema(), other.nameKey);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.nameType)) {
        this.nameType = data().deepCopy(fields()[13].schema(), other.nameType);
        fieldSetFlags()[13] = true;
      }
      if (isValidValue(fields()[14], other.authorship)) {
        this.authorship = data().deepCopy(fields()[14].schema(), other.authorship);
        fieldSetFlags()[14] = true;
      }
      if (isValidValue(fields()[15], other.origin)) {
        this.origin = data().deepCopy(fields()[15].schema(), other.origin);
        fieldSetFlags()[15] = true;
      }
      if (isValidValue(fields()[16], other.taxonomicStatus)) {
        this.taxonomicStatus = data().deepCopy(fields()[16].schema(), other.taxonomicStatus);
        fieldSetFlags()[16] = true;
      }
      if (isValidValue(fields()[17], other.nomenclaturalStatus)) {
        this.nomenclaturalStatus = data().deepCopy(fields()[17].schema(), other.nomenclaturalStatus);
        fieldSetFlags()[17] = true;
      }
      if (isValidValue(fields()[18], other.threatStatus)) {
        this.threatStatus = data().deepCopy(fields()[18].schema(), other.threatStatus);
        fieldSetFlags()[18] = true;
      }
      if (isValidValue(fields()[19], other.rank)) {
        this.rank = data().deepCopy(fields()[19].schema(), other.rank);
        fieldSetFlags()[19] = true;
      }
      if (isValidValue(fields()[20], other.rankKey)) {
        this.rankKey = data().deepCopy(fields()[20].schema(), other.rankKey);
        fieldSetFlags()[20] = true;
      }
      if (isValidValue(fields()[21], other.habitat)) {
        this.habitat = data().deepCopy(fields()[21].schema(), other.habitat);
        fieldSetFlags()[21] = true;
      }
      if (isValidValue(fields()[22], other.publishedIn)) {
        this.publishedIn = data().deepCopy(fields()[22].schema(), other.publishedIn);
        fieldSetFlags()[22] = true;
      }
      if (isValidValue(fields()[23], other.accordingTo)) {
        this.accordingTo = data().deepCopy(fields()[23].schema(), other.accordingTo);
        fieldSetFlags()[23] = true;
      }
      if (isValidValue(fields()[24], other.kingdomKey)) {
        this.kingdomKey = data().deepCopy(fields()[24].schema(), other.kingdomKey);
        fieldSetFlags()[24] = true;
      }
      if (isValidValue(fields()[25], other.kingdom)) {
        this.kingdom = data().deepCopy(fields()[25].schema(), other.kingdom);
        fieldSetFlags()[25] = true;
      }
      if (isValidValue(fields()[26], other.phylumKey)) {
        this.phylumKey = data().deepCopy(fields()[26].schema(), other.phylumKey);
        fieldSetFlags()[26] = true;
      }
      if (isValidValue(fields()[27], other.phylum)) {
        this.phylum = data().deepCopy(fields()[27].schema(), other.phylum);
        fieldSetFlags()[27] = true;
      }
      if (isValidValue(fields()[28], other.classKey)) {
        this.classKey = data().deepCopy(fields()[28].schema(), other.classKey);
        fieldSetFlags()[28] = true;
      }
      if (isValidValue(fields()[29], other.clazz)) {
        this.clazz = data().deepCopy(fields()[29].schema(), other.clazz);
        fieldSetFlags()[29] = true;
      }
      if (isValidValue(fields()[30], other.orderKey)) {
        this.orderKey = data().deepCopy(fields()[30].schema(), other.orderKey);
        fieldSetFlags()[30] = true;
      }
      if (isValidValue(fields()[31], other.order)) {
        this.order = data().deepCopy(fields()[31].schema(), other.order);
        fieldSetFlags()[31] = true;
      }
      if (isValidValue(fields()[32], other.familyKey)) {
        this.familyKey = data().deepCopy(fields()[32].schema(), other.familyKey);
        fieldSetFlags()[32] = true;
      }
      if (isValidValue(fields()[33], other.family)) {
        this.family = data().deepCopy(fields()[33].schema(), other.family);
        fieldSetFlags()[33] = true;
      }
      if (isValidValue(fields()[34], other.genusKey)) {
        this.genusKey = data().deepCopy(fields()[34].schema(), other.genusKey);
        fieldSetFlags()[34] = true;
      }
      if (isValidValue(fields()[35], other.genus)) {
        this.genus = data().deepCopy(fields()[35].schema(), other.genus);
        fieldSetFlags()[35] = true;
      }
      if (isValidValue(fields()[36], other.subgenusKey)) {
        this.subgenusKey = data().deepCopy(fields()[36].schema(), other.subgenusKey);
        fieldSetFlags()[36] = true;
      }
      if (isValidValue(fields()[37], other.subgenus)) {
        this.subgenus = data().deepCopy(fields()[37].schema(), other.subgenus);
        fieldSetFlags()[37] = true;
      }
      if (isValidValue(fields()[38], other.speciesKey)) {
        this.speciesKey = data().deepCopy(fields()[38].schema(), other.speciesKey);
        fieldSetFlags()[38] = true;
      }
      if (isValidValue(fields()[39], other.species)) {
        this.species = data().deepCopy(fields()[39].schema(), other.species);
        fieldSetFlags()[39] = true;
      }
      if (isValidValue(fields()[40], other.numDescendants)) {
        this.numDescendants = data().deepCopy(fields()[40].schema(), other.numDescendants);
        fieldSetFlags()[40] = true;
      }
      if (isValidValue(fields()[41], other.sourceId)) {
        this.sourceId = data().deepCopy(fields()[41].schema(), other.sourceId);
        fieldSetFlags()[41] = true;
      }
      if (isValidValue(fields()[42], other.isSynonym)) {
        this.isSynonym = data().deepCopy(fields()[42].schema(), other.isSynonym);
        fieldSetFlags()[42] = true;
      }
      if (isValidValue(fields()[43], other.extinct)) {
        this.extinct = data().deepCopy(fields()[43].schema(), other.extinct);
        fieldSetFlags()[43] = true;
      }
      if (isValidValue(fields()[44], other.description)) {
        this.description = data().deepCopy(fields()[44].schema(), other.description);
        fieldSetFlags()[44] = true;
      }
      if (isValidValue(fields()[45], other.vernacularName)) {
        this.vernacularName = data().deepCopy(fields()[45].schema(), other.vernacularName);
        fieldSetFlags()[45] = true;
      }
      if (isValidValue(fields()[46], other.vernacularLang)) {
        this.vernacularLang = data().deepCopy(fields()[46].schema(), other.vernacularLang);
        fieldSetFlags()[46] = true;
      }
      if (isValidValue(fields()[47], other.vernacularNameLang)) {
        this.vernacularNameLang = data().deepCopy(fields()[47].schema(), other.vernacularNameLang);
        fieldSetFlags()[47] = true;
      }
      if (isValidValue(fields()[48], other.higherTaxonKey)) {
        this.higherTaxonKey = data().deepCopy(fields()[48].schema(), other.higherTaxonKey);
        fieldSetFlags()[48] = true;
      }
      if (isValidValue(fields()[49], other.issues)) {
        this.issues = data().deepCopy(fields()[49].schema(), other.issues);
        fieldSetFlags()[49] = true;
      }
    }

    /**
      * Gets the value of the 'key' field.
      * @return The value.
      */
    public java.lang.Integer getKey() {
      return key;
    }

    /**
      * Sets the value of the 'key' field.
      * @param value The value of 'key'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setKey(java.lang.Integer value) {
      validate(fields()[0], value);
      this.key = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'key' field has been set.
      * @return True if the 'key' field has been set, false otherwise.
      */
    public boolean hasKey() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'key' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearKey() {
      key = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'nubKey' field.
      * @return The value.
      */
    public java.lang.Integer getNubKey() {
      return nubKey;
    }

    /**
      * Sets the value of the 'nubKey' field.
      * @param value The value of 'nubKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNubKey(java.lang.Integer value) {
      validate(fields()[1], value);
      this.nubKey = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'nubKey' field has been set.
      * @return True if the 'nubKey' field has been set, false otherwise.
      */
    public boolean hasNubKey() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'nubKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNubKey() {
      nubKey = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'datasetKey' field.
      * @return The value.
      */
    public java.lang.String getDatasetKey() {
      return datasetKey;
    }

    /**
      * Sets the value of the 'datasetKey' field.
      * @param value The value of 'datasetKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setDatasetKey(java.lang.String value) {
      validate(fields()[2], value);
      this.datasetKey = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'datasetKey' field has been set.
      * @return True if the 'datasetKey' field has been set, false otherwise.
      */
    public boolean hasDatasetKey() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'datasetKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearDatasetKey() {
      datasetKey = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'constituentKey' field.
      * @return The value.
      */
    public java.lang.String getConstituentKey() {
      return constituentKey;
    }

    /**
      * Sets the value of the 'constituentKey' field.
      * @param value The value of 'constituentKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setConstituentKey(java.lang.String value) {
      validate(fields()[3], value);
      this.constituentKey = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'constituentKey' field has been set.
      * @return True if the 'constituentKey' field has been set, false otherwise.
      */
    public boolean hasConstituentKey() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'constituentKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearConstituentKey() {
      constituentKey = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'parentKey' field.
      * @return The value.
      */
    public java.lang.Integer getParentKey() {
      return parentKey;
    }

    /**
      * Sets the value of the 'parentKey' field.
      * @param value The value of 'parentKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setParentKey(java.lang.Integer value) {
      validate(fields()[4], value);
      this.parentKey = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'parentKey' field has been set.
      * @return True if the 'parentKey' field has been set, false otherwise.
      */
    public boolean hasParentKey() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'parentKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearParentKey() {
      parentKey = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'parent' field.
      * @return The value.
      */
    public java.lang.String getParent() {
      return parent;
    }

    /**
      * Sets the value of the 'parent' field.
      * @param value The value of 'parent'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setParent(java.lang.String value) {
      validate(fields()[5], value);
      this.parent = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'parent' field has been set.
      * @return True if the 'parent' field has been set, false otherwise.
      */
    public boolean hasParent() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'parent' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearParent() {
      parent = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'acceptedKey' field.
      * @return The value.
      */
    public java.lang.Integer getAcceptedKey() {
      return acceptedKey;
    }

    /**
      * Sets the value of the 'acceptedKey' field.
      * @param value The value of 'acceptedKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAcceptedKey(java.lang.Integer value) {
      validate(fields()[6], value);
      this.acceptedKey = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'acceptedKey' field has been set.
      * @return True if the 'acceptedKey' field has been set, false otherwise.
      */
    public boolean hasAcceptedKey() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'acceptedKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAcceptedKey() {
      acceptedKey = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /**
      * Gets the value of the 'accepted' field.
      * @return The value.
      */
    public java.lang.String getAccepted() {
      return accepted;
    }

    /**
      * Sets the value of the 'accepted' field.
      * @param value The value of 'accepted'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAccepted(java.lang.String value) {
      validate(fields()[7], value);
      this.accepted = value;
      fieldSetFlags()[7] = true;
      return this;
    }

    /**
      * Checks whether the 'accepted' field has been set.
      * @return True if the 'accepted' field has been set, false otherwise.
      */
    public boolean hasAccepted() {
      return fieldSetFlags()[7];
    }


    /**
      * Clears the value of the 'accepted' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAccepted() {
      accepted = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    /**
      * Gets the value of the 'basionymKey' field.
      * @return The value.
      */
    public java.lang.Integer getBasionymKey() {
      return basionymKey;
    }

    /**
      * Sets the value of the 'basionymKey' field.
      * @param value The value of 'basionymKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setBasionymKey(java.lang.Integer value) {
      validate(fields()[8], value);
      this.basionymKey = value;
      fieldSetFlags()[8] = true;
      return this;
    }

    /**
      * Checks whether the 'basionymKey' field has been set.
      * @return True if the 'basionymKey' field has been set, false otherwise.
      */
    public boolean hasBasionymKey() {
      return fieldSetFlags()[8];
    }


    /**
      * Clears the value of the 'basionymKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearBasionymKey() {
      basionymKey = null;
      fieldSetFlags()[8] = false;
      return this;
    }

    /**
      * Gets the value of the 'basionym' field.
      * @return The value.
      */
    public java.lang.String getBasionym() {
      return basionym;
    }

    /**
      * Sets the value of the 'basionym' field.
      * @param value The value of 'basionym'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setBasionym(java.lang.String value) {
      validate(fields()[9], value);
      this.basionym = value;
      fieldSetFlags()[9] = true;
      return this;
    }

    /**
      * Checks whether the 'basionym' field has been set.
      * @return True if the 'basionym' field has been set, false otherwise.
      */
    public boolean hasBasionym() {
      return fieldSetFlags()[9];
    }


    /**
      * Clears the value of the 'basionym' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearBasionym() {
      basionym = null;
      fieldSetFlags()[9] = false;
      return this;
    }

    /**
      * Gets the value of the 'scientificName' field.
      * @return The value.
      */
    public java.lang.String getScientificName() {
      return scientificName;
    }

    /**
      * Sets the value of the 'scientificName' field.
      * @param value The value of 'scientificName'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setScientificName(java.lang.String value) {
      validate(fields()[10], value);
      this.scientificName = value;
      fieldSetFlags()[10] = true;
      return this;
    }

    /**
      * Checks whether the 'scientificName' field has been set.
      * @return True if the 'scientificName' field has been set, false otherwise.
      */
    public boolean hasScientificName() {
      return fieldSetFlags()[10];
    }


    /**
      * Clears the value of the 'scientificName' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearScientificName() {
      scientificName = null;
      fieldSetFlags()[10] = false;
      return this;
    }

    /**
      * Gets the value of the 'canonicalName' field.
      * @return The value.
      */
    public java.lang.String getCanonicalName() {
      return canonicalName;
    }

    /**
      * Sets the value of the 'canonicalName' field.
      * @param value The value of 'canonicalName'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setCanonicalName(java.lang.String value) {
      validate(fields()[11], value);
      this.canonicalName = value;
      fieldSetFlags()[11] = true;
      return this;
    }

    /**
      * Checks whether the 'canonicalName' field has been set.
      * @return True if the 'canonicalName' field has been set, false otherwise.
      */
    public boolean hasCanonicalName() {
      return fieldSetFlags()[11];
    }


    /**
      * Clears the value of the 'canonicalName' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearCanonicalName() {
      canonicalName = null;
      fieldSetFlags()[11] = false;
      return this;
    }

    /**
      * Gets the value of the 'nameKey' field.
      * @return The value.
      */
    public java.lang.Integer getNameKey() {
      return nameKey;
    }

    /**
      * Sets the value of the 'nameKey' field.
      * @param value The value of 'nameKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNameKey(java.lang.Integer value) {
      validate(fields()[12], value);
      this.nameKey = value;
      fieldSetFlags()[12] = true;
      return this;
    }

    /**
      * Checks whether the 'nameKey' field has been set.
      * @return True if the 'nameKey' field has been set, false otherwise.
      */
    public boolean hasNameKey() {
      return fieldSetFlags()[12];
    }


    /**
      * Clears the value of the 'nameKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNameKey() {
      nameKey = null;
      fieldSetFlags()[12] = false;
      return this;
    }

    /**
      * Gets the value of the 'nameType' field.
      * @return The value.
      */
    public java.lang.String getNameType() {
      return nameType;
    }

    /**
      * Sets the value of the 'nameType' field.
      * @param value The value of 'nameType'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNameType(java.lang.String value) {
      validate(fields()[13], value);
      this.nameType = value;
      fieldSetFlags()[13] = true;
      return this;
    }

    /**
      * Checks whether the 'nameType' field has been set.
      * @return True if the 'nameType' field has been set, false otherwise.
      */
    public boolean hasNameType() {
      return fieldSetFlags()[13];
    }


    /**
      * Clears the value of the 'nameType' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNameType() {
      nameType = null;
      fieldSetFlags()[13] = false;
      return this;
    }

    /**
      * Gets the value of the 'authorship' field.
      * @return The value.
      */
    public java.lang.String getAuthorship() {
      return authorship;
    }

    /**
      * Sets the value of the 'authorship' field.
      * @param value The value of 'authorship'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAuthorship(java.lang.String value) {
      validate(fields()[14], value);
      this.authorship = value;
      fieldSetFlags()[14] = true;
      return this;
    }

    /**
      * Checks whether the 'authorship' field has been set.
      * @return True if the 'authorship' field has been set, false otherwise.
      */
    public boolean hasAuthorship() {
      return fieldSetFlags()[14];
    }


    /**
      * Clears the value of the 'authorship' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAuthorship() {
      authorship = null;
      fieldSetFlags()[14] = false;
      return this;
    }

    /**
      * Gets the value of the 'origin' field.
      * @return The value.
      */
    public java.lang.String getOrigin() {
      return origin;
    }

    /**
      * Sets the value of the 'origin' field.
      * @param value The value of 'origin'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setOrigin(java.lang.String value) {
      validate(fields()[15], value);
      this.origin = value;
      fieldSetFlags()[15] = true;
      return this;
    }

    /**
      * Checks whether the 'origin' field has been set.
      * @return True if the 'origin' field has been set, false otherwise.
      */
    public boolean hasOrigin() {
      return fieldSetFlags()[15];
    }


    /**
      * Clears the value of the 'origin' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearOrigin() {
      origin = null;
      fieldSetFlags()[15] = false;
      return this;
    }

    /**
      * Gets the value of the 'taxonomicStatus' field.
      * @return The value.
      */
    public java.lang.String getTaxonomicStatus() {
      return taxonomicStatus;
    }

    /**
      * Sets the value of the 'taxonomicStatus' field.
      * @param value The value of 'taxonomicStatus'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setTaxonomicStatus(java.lang.String value) {
      validate(fields()[16], value);
      this.taxonomicStatus = value;
      fieldSetFlags()[16] = true;
      return this;
    }

    /**
      * Checks whether the 'taxonomicStatus' field has been set.
      * @return True if the 'taxonomicStatus' field has been set, false otherwise.
      */
    public boolean hasTaxonomicStatus() {
      return fieldSetFlags()[16];
    }


    /**
      * Clears the value of the 'taxonomicStatus' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearTaxonomicStatus() {
      taxonomicStatus = null;
      fieldSetFlags()[16] = false;
      return this;
    }

    /**
      * Gets the value of the 'nomenclaturalStatus' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getNomenclaturalStatus() {
      return nomenclaturalStatus;
    }

    /**
      * Sets the value of the 'nomenclaturalStatus' field.
      * @param value The value of 'nomenclaturalStatus'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNomenclaturalStatus(java.util.List<java.lang.String> value) {
      validate(fields()[17], value);
      this.nomenclaturalStatus = value;
      fieldSetFlags()[17] = true;
      return this;
    }

    /**
      * Checks whether the 'nomenclaturalStatus' field has been set.
      * @return True if the 'nomenclaturalStatus' field has been set, false otherwise.
      */
    public boolean hasNomenclaturalStatus() {
      return fieldSetFlags()[17];
    }


    /**
      * Clears the value of the 'nomenclaturalStatus' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNomenclaturalStatus() {
      nomenclaturalStatus = null;
      fieldSetFlags()[17] = false;
      return this;
    }

    /**
      * Gets the value of the 'threatStatus' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getThreatStatus() {
      return threatStatus;
    }

    /**
      * Sets the value of the 'threatStatus' field.
      * @param value The value of 'threatStatus'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setThreatStatus(java.util.List<java.lang.String> value) {
      validate(fields()[18], value);
      this.threatStatus = value;
      fieldSetFlags()[18] = true;
      return this;
    }

    /**
      * Checks whether the 'threatStatus' field has been set.
      * @return True if the 'threatStatus' field has been set, false otherwise.
      */
    public boolean hasThreatStatus() {
      return fieldSetFlags()[18];
    }


    /**
      * Clears the value of the 'threatStatus' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearThreatStatus() {
      threatStatus = null;
      fieldSetFlags()[18] = false;
      return this;
    }

    /**
      * Gets the value of the 'rank' field.
      * @return The value.
      */
    public java.lang.String getRank() {
      return rank;
    }

    /**
      * Sets the value of the 'rank' field.
      * @param value The value of 'rank'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setRank(java.lang.String value) {
      validate(fields()[19], value);
      this.rank = value;
      fieldSetFlags()[19] = true;
      return this;
    }

    /**
      * Checks whether the 'rank' field has been set.
      * @return True if the 'rank' field has been set, false otherwise.
      */
    public boolean hasRank() {
      return fieldSetFlags()[19];
    }


    /**
      * Clears the value of the 'rank' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearRank() {
      rank = null;
      fieldSetFlags()[19] = false;
      return this;
    }

    /**
      * Gets the value of the 'rankKey' field.
      * @return The value.
      */
    public java.lang.Integer getRankKey() {
      return rankKey;
    }

    /**
      * Sets the value of the 'rankKey' field.
      * @param value The value of 'rankKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setRankKey(java.lang.Integer value) {
      validate(fields()[20], value);
      this.rankKey = value;
      fieldSetFlags()[20] = true;
      return this;
    }

    /**
      * Checks whether the 'rankKey' field has been set.
      * @return True if the 'rankKey' field has been set, false otherwise.
      */
    public boolean hasRankKey() {
      return fieldSetFlags()[20];
    }


    /**
      * Clears the value of the 'rankKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearRankKey() {
      rankKey = null;
      fieldSetFlags()[20] = false;
      return this;
    }

    /**
      * Gets the value of the 'habitat' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getHabitat() {
      return habitat;
    }

    /**
      * Sets the value of the 'habitat' field.
      * @param value The value of 'habitat'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setHabitat(java.util.List<java.lang.String> value) {
      validate(fields()[21], value);
      this.habitat = value;
      fieldSetFlags()[21] = true;
      return this;
    }

    /**
      * Checks whether the 'habitat' field has been set.
      * @return True if the 'habitat' field has been set, false otherwise.
      */
    public boolean hasHabitat() {
      return fieldSetFlags()[21];
    }


    /**
      * Clears the value of the 'habitat' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearHabitat() {
      habitat = null;
      fieldSetFlags()[21] = false;
      return this;
    }

    /**
      * Gets the value of the 'publishedIn' field.
      * @return The value.
      */
    public java.lang.String getPublishedIn() {
      return publishedIn;
    }

    /**
      * Sets the value of the 'publishedIn' field.
      * @param value The value of 'publishedIn'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setPublishedIn(java.lang.String value) {
      validate(fields()[22], value);
      this.publishedIn = value;
      fieldSetFlags()[22] = true;
      return this;
    }

    /**
      * Checks whether the 'publishedIn' field has been set.
      * @return True if the 'publishedIn' field has been set, false otherwise.
      */
    public boolean hasPublishedIn() {
      return fieldSetFlags()[22];
    }


    /**
      * Clears the value of the 'publishedIn' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearPublishedIn() {
      publishedIn = null;
      fieldSetFlags()[22] = false;
      return this;
    }

    /**
      * Gets the value of the 'accordingTo' field.
      * @return The value.
      */
    public java.lang.String getAccordingTo() {
      return accordingTo;
    }

    /**
      * Sets the value of the 'accordingTo' field.
      * @param value The value of 'accordingTo'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAccordingTo(java.lang.String value) {
      validate(fields()[23], value);
      this.accordingTo = value;
      fieldSetFlags()[23] = true;
      return this;
    }

    /**
      * Checks whether the 'accordingTo' field has been set.
      * @return True if the 'accordingTo' field has been set, false otherwise.
      */
    public boolean hasAccordingTo() {
      return fieldSetFlags()[23];
    }


    /**
      * Clears the value of the 'accordingTo' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAccordingTo() {
      accordingTo = null;
      fieldSetFlags()[23] = false;
      return this;
    }

    /**
      * Gets the value of the 'kingdomKey' field.
      * @return The value.
      */
    public java.lang.Integer getKingdomKey() {
      return kingdomKey;
    }

    /**
      * Sets the value of the 'kingdomKey' field.
      * @param value The value of 'kingdomKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setKingdomKey(java.lang.Integer value) {
      validate(fields()[24], value);
      this.kingdomKey = value;
      fieldSetFlags()[24] = true;
      return this;
    }

    /**
      * Checks whether the 'kingdomKey' field has been set.
      * @return True if the 'kingdomKey' field has been set, false otherwise.
      */
    public boolean hasKingdomKey() {
      return fieldSetFlags()[24];
    }


    /**
      * Clears the value of the 'kingdomKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearKingdomKey() {
      kingdomKey = null;
      fieldSetFlags()[24] = false;
      return this;
    }

    /**
      * Gets the value of the 'kingdom' field.
      * @return The value.
      */
    public java.lang.String getKingdom() {
      return kingdom;
    }

    /**
      * Sets the value of the 'kingdom' field.
      * @param value The value of 'kingdom'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setKingdom(java.lang.String value) {
      validate(fields()[25], value);
      this.kingdom = value;
      fieldSetFlags()[25] = true;
      return this;
    }

    /**
      * Checks whether the 'kingdom' field has been set.
      * @return True if the 'kingdom' field has been set, false otherwise.
      */
    public boolean hasKingdom() {
      return fieldSetFlags()[25];
    }


    /**
      * Clears the value of the 'kingdom' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearKingdom() {
      kingdom = null;
      fieldSetFlags()[25] = false;
      return this;
    }

    /**
      * Gets the value of the 'phylumKey' field.
      * @return The value.
      */
    public java.lang.Integer getPhylumKey() {
      return phylumKey;
    }

    /**
      * Sets the value of the 'phylumKey' field.
      * @param value The value of 'phylumKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setPhylumKey(java.lang.Integer value) {
      validate(fields()[26], value);
      this.phylumKey = value;
      fieldSetFlags()[26] = true;
      return this;
    }

    /**
      * Checks whether the 'phylumKey' field has been set.
      * @return True if the 'phylumKey' field has been set, false otherwise.
      */
    public boolean hasPhylumKey() {
      return fieldSetFlags()[26];
    }


    /**
      * Clears the value of the 'phylumKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearPhylumKey() {
      phylumKey = null;
      fieldSetFlags()[26] = false;
      return this;
    }

    /**
      * Gets the value of the 'phylum' field.
      * @return The value.
      */
    public java.lang.String getPhylum() {
      return phylum;
    }

    /**
      * Sets the value of the 'phylum' field.
      * @param value The value of 'phylum'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setPhylum(java.lang.String value) {
      validate(fields()[27], value);
      this.phylum = value;
      fieldSetFlags()[27] = true;
      return this;
    }

    /**
      * Checks whether the 'phylum' field has been set.
      * @return True if the 'phylum' field has been set, false otherwise.
      */
    public boolean hasPhylum() {
      return fieldSetFlags()[27];
    }


    /**
      * Clears the value of the 'phylum' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearPhylum() {
      phylum = null;
      fieldSetFlags()[27] = false;
      return this;
    }

    /**
      * Gets the value of the 'classKey' field.
      * @return The value.
      */
    public java.lang.Integer getClassKey() {
      return classKey;
    }

    /**
      * Sets the value of the 'classKey' field.
      * @param value The value of 'classKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setClassKey(java.lang.Integer value) {
      validate(fields()[28], value);
      this.classKey = value;
      fieldSetFlags()[28] = true;
      return this;
    }

    /**
      * Checks whether the 'classKey' field has been set.
      * @return True if the 'classKey' field has been set, false otherwise.
      */
    public boolean hasClassKey() {
      return fieldSetFlags()[28];
    }


    /**
      * Clears the value of the 'classKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearClassKey() {
      classKey = null;
      fieldSetFlags()[28] = false;
      return this;
    }

    /**
      * Gets the value of the 'clazz' field.
      * @return The value.
      */
    public java.lang.String getClazz() {
      return clazz;
    }

    /**
      * Sets the value of the 'clazz' field.
      * @param value The value of 'clazz'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setClazz(java.lang.String value) {
      validate(fields()[29], value);
      this.clazz = value;
      fieldSetFlags()[29] = true;
      return this;
    }

    /**
      * Checks whether the 'clazz' field has been set.
      * @return True if the 'clazz' field has been set, false otherwise.
      */
    public boolean hasClazz() {
      return fieldSetFlags()[29];
    }


    /**
      * Clears the value of the 'clazz' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearClazz() {
      clazz = null;
      fieldSetFlags()[29] = false;
      return this;
    }

    /**
      * Gets the value of the 'orderKey' field.
      * @return The value.
      */
    public java.lang.Integer getOrderKey() {
      return orderKey;
    }

    /**
      * Sets the value of the 'orderKey' field.
      * @param value The value of 'orderKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setOrderKey(java.lang.Integer value) {
      validate(fields()[30], value);
      this.orderKey = value;
      fieldSetFlags()[30] = true;
      return this;
    }

    /**
      * Checks whether the 'orderKey' field has been set.
      * @return True if the 'orderKey' field has been set, false otherwise.
      */
    public boolean hasOrderKey() {
      return fieldSetFlags()[30];
    }


    /**
      * Clears the value of the 'orderKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearOrderKey() {
      orderKey = null;
      fieldSetFlags()[30] = false;
      return this;
    }

    /**
      * Gets the value of the 'order' field.
      * @return The value.
      */
    public java.lang.String getOrder() {
      return order;
    }

    /**
      * Sets the value of the 'order' field.
      * @param value The value of 'order'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setOrder(java.lang.String value) {
      validate(fields()[31], value);
      this.order = value;
      fieldSetFlags()[31] = true;
      return this;
    }

    /**
      * Checks whether the 'order' field has been set.
      * @return True if the 'order' field has been set, false otherwise.
      */
    public boolean hasOrder() {
      return fieldSetFlags()[31];
    }


    /**
      * Clears the value of the 'order' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearOrder() {
      order = null;
      fieldSetFlags()[31] = false;
      return this;
    }

    /**
      * Gets the value of the 'familyKey' field.
      * @return The value.
      */
    public java.lang.Integer getFamilyKey() {
      return familyKey;
    }

    /**
      * Sets the value of the 'familyKey' field.
      * @param value The value of 'familyKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setFamilyKey(java.lang.Integer value) {
      validate(fields()[32], value);
      this.familyKey = value;
      fieldSetFlags()[32] = true;
      return this;
    }

    /**
      * Checks whether the 'familyKey' field has been set.
      * @return True if the 'familyKey' field has been set, false otherwise.
      */
    public boolean hasFamilyKey() {
      return fieldSetFlags()[32];
    }


    /**
      * Clears the value of the 'familyKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearFamilyKey() {
      familyKey = null;
      fieldSetFlags()[32] = false;
      return this;
    }

    /**
      * Gets the value of the 'family' field.
      * @return The value.
      */
    public java.lang.String getFamily() {
      return family;
    }

    /**
      * Sets the value of the 'family' field.
      * @param value The value of 'family'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setFamily(java.lang.String value) {
      validate(fields()[33], value);
      this.family = value;
      fieldSetFlags()[33] = true;
      return this;
    }

    /**
      * Checks whether the 'family' field has been set.
      * @return True if the 'family' field has been set, false otherwise.
      */
    public boolean hasFamily() {
      return fieldSetFlags()[33];
    }


    /**
      * Clears the value of the 'family' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearFamily() {
      family = null;
      fieldSetFlags()[33] = false;
      return this;
    }

    /**
      * Gets the value of the 'genusKey' field.
      * @return The value.
      */
    public java.lang.Integer getGenusKey() {
      return genusKey;
    }

    /**
      * Sets the value of the 'genusKey' field.
      * @param value The value of 'genusKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setGenusKey(java.lang.Integer value) {
      validate(fields()[34], value);
      this.genusKey = value;
      fieldSetFlags()[34] = true;
      return this;
    }

    /**
      * Checks whether the 'genusKey' field has been set.
      * @return True if the 'genusKey' field has been set, false otherwise.
      */
    public boolean hasGenusKey() {
      return fieldSetFlags()[34];
    }


    /**
      * Clears the value of the 'genusKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearGenusKey() {
      genusKey = null;
      fieldSetFlags()[34] = false;
      return this;
    }

    /**
      * Gets the value of the 'genus' field.
      * @return The value.
      */
    public java.lang.String getGenus() {
      return genus;
    }

    /**
      * Sets the value of the 'genus' field.
      * @param value The value of 'genus'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setGenus(java.lang.String value) {
      validate(fields()[35], value);
      this.genus = value;
      fieldSetFlags()[35] = true;
      return this;
    }

    /**
      * Checks whether the 'genus' field has been set.
      * @return True if the 'genus' field has been set, false otherwise.
      */
    public boolean hasGenus() {
      return fieldSetFlags()[35];
    }


    /**
      * Clears the value of the 'genus' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearGenus() {
      genus = null;
      fieldSetFlags()[35] = false;
      return this;
    }

    /**
      * Gets the value of the 'subgenusKey' field.
      * @return The value.
      */
    public java.lang.Integer getSubgenusKey() {
      return subgenusKey;
    }

    /**
      * Sets the value of the 'subgenusKey' field.
      * @param value The value of 'subgenusKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSubgenusKey(java.lang.Integer value) {
      validate(fields()[36], value);
      this.subgenusKey = value;
      fieldSetFlags()[36] = true;
      return this;
    }

    /**
      * Checks whether the 'subgenusKey' field has been set.
      * @return True if the 'subgenusKey' field has been set, false otherwise.
      */
    public boolean hasSubgenusKey() {
      return fieldSetFlags()[36];
    }


    /**
      * Clears the value of the 'subgenusKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSubgenusKey() {
      subgenusKey = null;
      fieldSetFlags()[36] = false;
      return this;
    }

    /**
      * Gets the value of the 'subgenus' field.
      * @return The value.
      */
    public java.lang.String getSubgenus() {
      return subgenus;
    }

    /**
      * Sets the value of the 'subgenus' field.
      * @param value The value of 'subgenus'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSubgenus(java.lang.String value) {
      validate(fields()[37], value);
      this.subgenus = value;
      fieldSetFlags()[37] = true;
      return this;
    }

    /**
      * Checks whether the 'subgenus' field has been set.
      * @return True if the 'subgenus' field has been set, false otherwise.
      */
    public boolean hasSubgenus() {
      return fieldSetFlags()[37];
    }


    /**
      * Clears the value of the 'subgenus' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSubgenus() {
      subgenus = null;
      fieldSetFlags()[37] = false;
      return this;
    }

    /**
      * Gets the value of the 'speciesKey' field.
      * @return The value.
      */
    public java.lang.Integer getSpeciesKey() {
      return speciesKey;
    }

    /**
      * Sets the value of the 'speciesKey' field.
      * @param value The value of 'speciesKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSpeciesKey(java.lang.Integer value) {
      validate(fields()[38], value);
      this.speciesKey = value;
      fieldSetFlags()[38] = true;
      return this;
    }

    /**
      * Checks whether the 'speciesKey' field has been set.
      * @return True if the 'speciesKey' field has been set, false otherwise.
      */
    public boolean hasSpeciesKey() {
      return fieldSetFlags()[38];
    }


    /**
      * Clears the value of the 'speciesKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSpeciesKey() {
      speciesKey = null;
      fieldSetFlags()[38] = false;
      return this;
    }

    /**
      * Gets the value of the 'species' field.
      * @return The value.
      */
    public java.lang.String getSpecies() {
      return species;
    }

    /**
      * Sets the value of the 'species' field.
      * @param value The value of 'species'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSpecies(java.lang.String value) {
      validate(fields()[39], value);
      this.species = value;
      fieldSetFlags()[39] = true;
      return this;
    }

    /**
      * Checks whether the 'species' field has been set.
      * @return True if the 'species' field has been set, false otherwise.
      */
    public boolean hasSpecies() {
      return fieldSetFlags()[39];
    }


    /**
      * Clears the value of the 'species' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSpecies() {
      species = null;
      fieldSetFlags()[39] = false;
      return this;
    }

    /**
      * Gets the value of the 'numDescendants' field.
      * @return The value.
      */
    public java.lang.Integer getNumDescendants() {
      return numDescendants;
    }

    /**
      * Sets the value of the 'numDescendants' field.
      * @param value The value of 'numDescendants'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNumDescendants(java.lang.Integer value) {
      validate(fields()[40], value);
      this.numDescendants = value;
      fieldSetFlags()[40] = true;
      return this;
    }

    /**
      * Checks whether the 'numDescendants' field has been set.
      * @return True if the 'numDescendants' field has been set, false otherwise.
      */
    public boolean hasNumDescendants() {
      return fieldSetFlags()[40];
    }


    /**
      * Clears the value of the 'numDescendants' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNumDescendants() {
      numDescendants = null;
      fieldSetFlags()[40] = false;
      return this;
    }

    /**
      * Gets the value of the 'sourceId' field.
      * @return The value.
      */
    public java.lang.String getSourceId() {
      return sourceId;
    }

    /**
      * Sets the value of the 'sourceId' field.
      * @param value The value of 'sourceId'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSourceId(java.lang.String value) {
      validate(fields()[41], value);
      this.sourceId = value;
      fieldSetFlags()[41] = true;
      return this;
    }

    /**
      * Checks whether the 'sourceId' field has been set.
      * @return True if the 'sourceId' field has been set, false otherwise.
      */
    public boolean hasSourceId() {
      return fieldSetFlags()[41];
    }


    /**
      * Clears the value of the 'sourceId' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSourceId() {
      sourceId = null;
      fieldSetFlags()[41] = false;
      return this;
    }

    /**
      * Gets the value of the 'isSynonym' field.
      * @return The value.
      */
    public java.lang.Boolean getIsSynonym() {
      return isSynonym;
    }

    /**
      * Sets the value of the 'isSynonym' field.
      * @param value The value of 'isSynonym'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setIsSynonym(java.lang.Boolean value) {
      validate(fields()[42], value);
      this.isSynonym = value;
      fieldSetFlags()[42] = true;
      return this;
    }

    /**
      * Checks whether the 'isSynonym' field has been set.
      * @return True if the 'isSynonym' field has been set, false otherwise.
      */
    public boolean hasIsSynonym() {
      return fieldSetFlags()[42];
    }


    /**
      * Clears the value of the 'isSynonym' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearIsSynonym() {
      isSynonym = null;
      fieldSetFlags()[42] = false;
      return this;
    }

    /**
      * Gets the value of the 'extinct' field.
      * @return The value.
      */
    public java.lang.Boolean getExtinct() {
      return extinct;
    }

    /**
      * Sets the value of the 'extinct' field.
      * @param value The value of 'extinct'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setExtinct(java.lang.Boolean value) {
      validate(fields()[43], value);
      this.extinct = value;
      fieldSetFlags()[43] = true;
      return this;
    }

    /**
      * Checks whether the 'extinct' field has been set.
      * @return True if the 'extinct' field has been set, false otherwise.
      */
    public boolean hasExtinct() {
      return fieldSetFlags()[43];
    }


    /**
      * Clears the value of the 'extinct' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearExtinct() {
      extinct = null;
      fieldSetFlags()[43] = false;
      return this;
    }

    /**
      * Gets the value of the 'description' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getDescription() {
      return description;
    }

    /**
      * Sets the value of the 'description' field.
      * @param value The value of 'description'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setDescription(java.util.List<java.lang.String> value) {
      validate(fields()[44], value);
      this.description = value;
      fieldSetFlags()[44] = true;
      return this;
    }

    /**
      * Checks whether the 'description' field has been set.
      * @return True if the 'description' field has been set, false otherwise.
      */
    public boolean hasDescription() {
      return fieldSetFlags()[44];
    }


    /**
      * Clears the value of the 'description' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearDescription() {
      description = null;
      fieldSetFlags()[44] = false;
      return this;
    }

    /**
      * Gets the value of the 'vernacularName' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getVernacularName() {
      return vernacularName;
    }

    /**
      * Sets the value of the 'vernacularName' field.
      * @param value The value of 'vernacularName'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setVernacularName(java.util.List<java.lang.String> value) {
      validate(fields()[45], value);
      this.vernacularName = value;
      fieldSetFlags()[45] = true;
      return this;
    }

    /**
      * Checks whether the 'vernacularName' field has been set.
      * @return True if the 'vernacularName' field has been set, false otherwise.
      */
    public boolean hasVernacularName() {
      return fieldSetFlags()[45];
    }


    /**
      * Clears the value of the 'vernacularName' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearVernacularName() {
      vernacularName = null;
      fieldSetFlags()[45] = false;
      return this;
    }

    /**
      * Gets the value of the 'vernacularLang' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getVernacularLang() {
      return vernacularLang;
    }

    /**
      * Sets the value of the 'vernacularLang' field.
      * @param value The value of 'vernacularLang'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setVernacularLang(java.util.List<java.lang.String> value) {
      validate(fields()[46], value);
      this.vernacularLang = value;
      fieldSetFlags()[46] = true;
      return this;
    }

    /**
      * Checks whether the 'vernacularLang' field has been set.
      * @return True if the 'vernacularLang' field has been set, false otherwise.
      */
    public boolean hasVernacularLang() {
      return fieldSetFlags()[46];
    }


    /**
      * Clears the value of the 'vernacularLang' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearVernacularLang() {
      vernacularLang = null;
      fieldSetFlags()[46] = false;
      return this;
    }

    /**
      * Gets the value of the 'vernacularNameLang' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getVernacularNameLang() {
      return vernacularNameLang;
    }

    /**
      * Sets the value of the 'vernacularNameLang' field.
      * @param value The value of 'vernacularNameLang'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setVernacularNameLang(java.util.List<java.lang.String> value) {
      validate(fields()[47], value);
      this.vernacularNameLang = value;
      fieldSetFlags()[47] = true;
      return this;
    }

    /**
      * Checks whether the 'vernacularNameLang' field has been set.
      * @return True if the 'vernacularNameLang' field has been set, false otherwise.
      */
    public boolean hasVernacularNameLang() {
      return fieldSetFlags()[47];
    }


    /**
      * Clears the value of the 'vernacularNameLang' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearVernacularNameLang() {
      vernacularNameLang = null;
      fieldSetFlags()[47] = false;
      return this;
    }

    /**
      * Gets the value of the 'higherTaxonKey' field.
      * @return The value.
      */
    public java.util.List<java.lang.Integer> getHigherTaxonKey() {
      return higherTaxonKey;
    }

    /**
      * Sets the value of the 'higherTaxonKey' field.
      * @param value The value of 'higherTaxonKey'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setHigherTaxonKey(java.util.List<java.lang.Integer> value) {
      validate(fields()[48], value);
      this.higherTaxonKey = value;
      fieldSetFlags()[48] = true;
      return this;
    }

    /**
      * Checks whether the 'higherTaxonKey' field has been set.
      * @return True if the 'higherTaxonKey' field has been set, false otherwise.
      */
    public boolean hasHigherTaxonKey() {
      return fieldSetFlags()[48];
    }


    /**
      * Clears the value of the 'higherTaxonKey' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearHigherTaxonKey() {
      higherTaxonKey = null;
      fieldSetFlags()[48] = false;
      return this;
    }

    /**
      * Gets the value of the 'issues' field.
      * @return The value.
      */
    public java.util.List<java.lang.String> getIssues() {
      return issues;
    }

    /**
      * Sets the value of the 'issues' field.
      * @param value The value of 'issues'.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setIssues(java.util.List<java.lang.String> value) {
      validate(fields()[49], value);
      this.issues = value;
      fieldSetFlags()[49] = true;
      return this;
    }

    /**
      * Checks whether the 'issues' field has been set.
      * @return True if the 'issues' field has been set, false otherwise.
      */
    public boolean hasIssues() {
      return fieldSetFlags()[49];
    }


    /**
      * Clears the value of the 'issues' field.
      * @return This builder.
      */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearIssues() {
      issues = null;
      fieldSetFlags()[49] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NameUsageAvro build() {
      try {
        NameUsageAvro record = new NameUsageAvro();
        record.key = fieldSetFlags()[0] ? this.key : (java.lang.Integer) defaultValue(fields()[0]);
        record.nubKey = fieldSetFlags()[1] ? this.nubKey : (java.lang.Integer) defaultValue(fields()[1]);
        record.datasetKey = fieldSetFlags()[2] ? this.datasetKey : (java.lang.String) defaultValue(fields()[2]);
        record.constituentKey = fieldSetFlags()[3] ? this.constituentKey : (java.lang.String) defaultValue(fields()[3]);
        record.parentKey = fieldSetFlags()[4] ? this.parentKey : (java.lang.Integer) defaultValue(fields()[4]);
        record.parent = fieldSetFlags()[5] ? this.parent : (java.lang.String) defaultValue(fields()[5]);
        record.acceptedKey = fieldSetFlags()[6] ? this.acceptedKey : (java.lang.Integer) defaultValue(fields()[6]);
        record.accepted = fieldSetFlags()[7] ? this.accepted : (java.lang.String) defaultValue(fields()[7]);
        record.basionymKey = fieldSetFlags()[8] ? this.basionymKey : (java.lang.Integer) defaultValue(fields()[8]);
        record.basionym = fieldSetFlags()[9] ? this.basionym : (java.lang.String) defaultValue(fields()[9]);
        record.scientificName = fieldSetFlags()[10] ? this.scientificName : (java.lang.String) defaultValue(fields()[10]);
        record.canonicalName = fieldSetFlags()[11] ? this.canonicalName : (java.lang.String) defaultValue(fields()[11]);
        record.nameKey = fieldSetFlags()[12] ? this.nameKey : (java.lang.Integer) defaultValue(fields()[12]);
        record.nameType = fieldSetFlags()[13] ? this.nameType : (java.lang.String) defaultValue(fields()[13]);
        record.authorship = fieldSetFlags()[14] ? this.authorship : (java.lang.String) defaultValue(fields()[14]);
        record.origin = fieldSetFlags()[15] ? this.origin : (java.lang.String) defaultValue(fields()[15]);
        record.taxonomicStatus = fieldSetFlags()[16] ? this.taxonomicStatus : (java.lang.String) defaultValue(fields()[16]);
        record.nomenclaturalStatus = fieldSetFlags()[17] ? this.nomenclaturalStatus : (java.util.List<java.lang.String>) defaultValue(fields()[17]);
        record.threatStatus = fieldSetFlags()[18] ? this.threatStatus : (java.util.List<java.lang.String>) defaultValue(fields()[18]);
        record.rank = fieldSetFlags()[19] ? this.rank : (java.lang.String) defaultValue(fields()[19]);
        record.rankKey = fieldSetFlags()[20] ? this.rankKey : (java.lang.Integer) defaultValue(fields()[20]);
        record.habitat = fieldSetFlags()[21] ? this.habitat : (java.util.List<java.lang.String>) defaultValue(fields()[21]);
        record.publishedIn = fieldSetFlags()[22] ? this.publishedIn : (java.lang.String) defaultValue(fields()[22]);
        record.accordingTo = fieldSetFlags()[23] ? this.accordingTo : (java.lang.String) defaultValue(fields()[23]);
        record.kingdomKey = fieldSetFlags()[24] ? this.kingdomKey : (java.lang.Integer) defaultValue(fields()[24]);
        record.kingdom = fieldSetFlags()[25] ? this.kingdom : (java.lang.String) defaultValue(fields()[25]);
        record.phylumKey = fieldSetFlags()[26] ? this.phylumKey : (java.lang.Integer) defaultValue(fields()[26]);
        record.phylum = fieldSetFlags()[27] ? this.phylum : (java.lang.String) defaultValue(fields()[27]);
        record.classKey = fieldSetFlags()[28] ? this.classKey : (java.lang.Integer) defaultValue(fields()[28]);
        record.clazz = fieldSetFlags()[29] ? this.clazz : (java.lang.String) defaultValue(fields()[29]);
        record.orderKey = fieldSetFlags()[30] ? this.orderKey : (java.lang.Integer) defaultValue(fields()[30]);
        record.order = fieldSetFlags()[31] ? this.order : (java.lang.String) defaultValue(fields()[31]);
        record.familyKey = fieldSetFlags()[32] ? this.familyKey : (java.lang.Integer) defaultValue(fields()[32]);
        record.family = fieldSetFlags()[33] ? this.family : (java.lang.String) defaultValue(fields()[33]);
        record.genusKey = fieldSetFlags()[34] ? this.genusKey : (java.lang.Integer) defaultValue(fields()[34]);
        record.genus = fieldSetFlags()[35] ? this.genus : (java.lang.String) defaultValue(fields()[35]);
        record.subgenusKey = fieldSetFlags()[36] ? this.subgenusKey : (java.lang.Integer) defaultValue(fields()[36]);
        record.subgenus = fieldSetFlags()[37] ? this.subgenus : (java.lang.String) defaultValue(fields()[37]);
        record.speciesKey = fieldSetFlags()[38] ? this.speciesKey : (java.lang.Integer) defaultValue(fields()[38]);
        record.species = fieldSetFlags()[39] ? this.species : (java.lang.String) defaultValue(fields()[39]);
        record.numDescendants = fieldSetFlags()[40] ? this.numDescendants : (java.lang.Integer) defaultValue(fields()[40]);
        record.sourceId = fieldSetFlags()[41] ? this.sourceId : (java.lang.String) defaultValue(fields()[41]);
        record.isSynonym = fieldSetFlags()[42] ? this.isSynonym : (java.lang.Boolean) defaultValue(fields()[42]);
        record.extinct = fieldSetFlags()[43] ? this.extinct : (java.lang.Boolean) defaultValue(fields()[43]);
        record.description = fieldSetFlags()[44] ? this.description : (java.util.List<java.lang.String>) defaultValue(fields()[44]);
        record.vernacularName = fieldSetFlags()[45] ? this.vernacularName : (java.util.List<java.lang.String>) defaultValue(fields()[45]);
        record.vernacularLang = fieldSetFlags()[46] ? this.vernacularLang : (java.util.List<java.lang.String>) defaultValue(fields()[46]);
        record.vernacularNameLang = fieldSetFlags()[47] ? this.vernacularNameLang : (java.util.List<java.lang.String>) defaultValue(fields()[47]);
        record.higherTaxonKey = fieldSetFlags()[48] ? this.higherTaxonKey : (java.util.List<java.lang.Integer>) defaultValue(fields()[48]);
        record.issues = fieldSetFlags()[49] ? this.issues : (java.util.List<java.lang.String>) defaultValue(fields()[49]);
        return record;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<NameUsageAvro>
    WRITER$ = (org.apache.avro.io.DatumWriter<NameUsageAvro>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<NameUsageAvro>
    READER$ = (org.apache.avro.io.DatumReader<NameUsageAvro>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}
