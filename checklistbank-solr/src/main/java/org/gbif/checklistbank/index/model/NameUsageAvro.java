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
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class NameUsageAvro extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"NameUsageAvro\",\"namespace\":\"org.gbif.checklistbank.index.model\",\"fields\":[{\"name\":\"key\",\"type\":[\"int\",\"null\"]},{\"name\":\"nubKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"datasetKey\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"constituentKey\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"parentKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"parent\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"acceptedKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"accepted\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"basionymKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"basionym\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"scientificName\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"canonicalName\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"nameType\",\"type\":[\"int\",\"null\"]},{\"name\":\"authorship\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"originKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"taxonomicStatusKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"nomenclaturalStatusKey\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}],\"default\":null},{\"name\":\"threatStatusKey\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}],\"default\":null},{\"name\":\"rankKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"habitatKey\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}],\"default\":null},{\"name\":\"publishedIn\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"accordingTo\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"kingdomKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"kingdom\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"phylumKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"phylum\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"classKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"clazz\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"orderKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"order\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"familyKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"family\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"genusKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"genus\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"subgenusKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"subgenus\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"speciesKey\",\"type\":[\"int\",\"null\"]},{\"name\":\"species\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"numDescendants\",\"type\":[\"int\",\"null\"]},{\"name\":\"sourceId\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"isSynonym\",\"type\":[\"boolean\",\"null\"]},{\"name\":\"extinct\",\"type\":[\"boolean\",\"null\"]},{\"name\":\"description\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"vernacularName\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"vernacularLang\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"vernacularNameLang\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}],\"default\":null},{\"name\":\"higherTaxonKey\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}],\"default\":null},{\"name\":\"issues\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}],\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
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
  @Deprecated public java.lang.Integer nameType;
  @Deprecated public java.lang.String authorship;
  @Deprecated public java.lang.Integer originKey;
  @Deprecated public java.lang.Integer taxonomicStatusKey;
  @Deprecated public java.util.List<java.lang.Integer> nomenclaturalStatusKey;
  @Deprecated public java.util.List<java.lang.Integer> threatStatusKey;
  @Deprecated public java.lang.Integer rankKey;
  @Deprecated public java.util.List<java.lang.Integer> habitatKey;
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
  @Deprecated public java.util.List<java.lang.Integer> issues;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public NameUsageAvro() {}

  /**
   * All-args constructor.
   */
  public NameUsageAvro(java.lang.Integer key, java.lang.Integer nubKey, java.lang.String datasetKey, java.lang.String constituentKey, java.lang.Integer parentKey, java.lang.String parent, java.lang.Integer acceptedKey, java.lang.String accepted, java.lang.Integer basionymKey, java.lang.String basionym, java.lang.String scientificName, java.lang.String canonicalName, java.lang.Integer nameType, java.lang.String authorship, java.lang.Integer originKey, java.lang.Integer taxonomicStatusKey, java.util.List<java.lang.Integer> nomenclaturalStatusKey, java.util.List<java.lang.Integer> threatStatusKey, java.lang.Integer rankKey, java.util.List<java.lang.Integer> habitatKey, java.lang.String publishedIn, java.lang.String accordingTo, java.lang.Integer kingdomKey, java.lang.String kingdom, java.lang.Integer phylumKey, java.lang.String phylum, java.lang.Integer classKey, java.lang.String clazz, java.lang.Integer orderKey, java.lang.String order, java.lang.Integer familyKey, java.lang.String family, java.lang.Integer genusKey, java.lang.String genus, java.lang.Integer subgenusKey, java.lang.String subgenus, java.lang.Integer speciesKey, java.lang.String species, java.lang.Integer numDescendants, java.lang.String sourceId, java.lang.Boolean isSynonym, java.lang.Boolean extinct, java.util.List<java.lang.String> description, java.util.List<java.lang.String> vernacularName, java.util.List<java.lang.String> vernacularLang, java.util.List<java.lang.String> vernacularNameLang, java.util.List<java.lang.Integer> higherTaxonKey, java.util.List<java.lang.Integer> issues) {
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
    this.nameType = nameType;
    this.authorship = authorship;
    this.originKey = originKey;
    this.taxonomicStatusKey = taxonomicStatusKey;
    this.nomenclaturalStatusKey = nomenclaturalStatusKey;
    this.threatStatusKey = threatStatusKey;
    this.rankKey = rankKey;
    this.habitatKey = habitatKey;
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
    case 12: return nameType;
    case 13: return authorship;
    case 14: return originKey;
    case 15: return taxonomicStatusKey;
    case 16: return nomenclaturalStatusKey;
    case 17: return threatStatusKey;
    case 18: return rankKey;
    case 19: return habitatKey;
    case 20: return publishedIn;
    case 21: return accordingTo;
    case 22: return kingdomKey;
    case 23: return kingdom;
    case 24: return phylumKey;
    case 25: return phylum;
    case 26: return classKey;
    case 27: return clazz;
    case 28: return orderKey;
    case 29: return order;
    case 30: return familyKey;
    case 31: return family;
    case 32: return genusKey;
    case 33: return genus;
    case 34: return subgenusKey;
    case 35: return subgenus;
    case 36: return speciesKey;
    case 37: return species;
    case 38: return numDescendants;
    case 39: return sourceId;
    case 40: return isSynonym;
    case 41: return extinct;
    case 42: return description;
    case 43: return vernacularName;
    case 44: return vernacularLang;
    case 45: return vernacularNameLang;
    case 46: return higherTaxonKey;
    case 47: return issues;
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
    case 12: nameType = (java.lang.Integer)value$; break;
    case 13: authorship = (java.lang.String)value$; break;
    case 14: originKey = (java.lang.Integer)value$; break;
    case 15: taxonomicStatusKey = (java.lang.Integer)value$; break;
    case 16: nomenclaturalStatusKey = (java.util.List<java.lang.Integer>)value$; break;
    case 17: threatStatusKey = (java.util.List<java.lang.Integer>)value$; break;
    case 18: rankKey = (java.lang.Integer)value$; break;
    case 19: habitatKey = (java.util.List<java.lang.Integer>)value$; break;
    case 20: publishedIn = (java.lang.String)value$; break;
    case 21: accordingTo = (java.lang.String)value$; break;
    case 22: kingdomKey = (java.lang.Integer)value$; break;
    case 23: kingdom = (java.lang.String)value$; break;
    case 24: phylumKey = (java.lang.Integer)value$; break;
    case 25: phylum = (java.lang.String)value$; break;
    case 26: classKey = (java.lang.Integer)value$; break;
    case 27: clazz = (java.lang.String)value$; break;
    case 28: orderKey = (java.lang.Integer)value$; break;
    case 29: order = (java.lang.String)value$; break;
    case 30: familyKey = (java.lang.Integer)value$; break;
    case 31: family = (java.lang.String)value$; break;
    case 32: genusKey = (java.lang.Integer)value$; break;
    case 33: genus = (java.lang.String)value$; break;
    case 34: subgenusKey = (java.lang.Integer)value$; break;
    case 35: subgenus = (java.lang.String)value$; break;
    case 36: speciesKey = (java.lang.Integer)value$; break;
    case 37: species = (java.lang.String)value$; break;
    case 38: numDescendants = (java.lang.Integer)value$; break;
    case 39: sourceId = (java.lang.String)value$; break;
    case 40: isSynonym = (java.lang.Boolean)value$; break;
    case 41: extinct = (java.lang.Boolean)value$; break;
    case 42: description = (java.util.List<java.lang.String>)value$; break;
    case 43: vernacularName = (java.util.List<java.lang.String>)value$; break;
    case 44: vernacularLang = (java.util.List<java.lang.String>)value$; break;
    case 45: vernacularNameLang = (java.util.List<java.lang.String>)value$; break;
    case 46: higherTaxonKey = (java.util.List<java.lang.Integer>)value$; break;
    case 47: issues = (java.util.List<java.lang.Integer>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'key' field.
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
   * Gets the value of the 'nameType' field.
   */
  public java.lang.Integer getNameType() {
    return nameType;
  }

  /**
   * Sets the value of the 'nameType' field.
   * @param value the value to set.
   */
  public void setNameType(java.lang.Integer value) {
    this.nameType = value;
  }

  /**
   * Gets the value of the 'authorship' field.
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
   * Gets the value of the 'originKey' field.
   */
  public java.lang.Integer getOriginKey() {
    return originKey;
  }

  /**
   * Sets the value of the 'originKey' field.
   * @param value the value to set.
   */
  public void setOriginKey(java.lang.Integer value) {
    this.originKey = value;
  }

  /**
   * Gets the value of the 'taxonomicStatusKey' field.
   */
  public java.lang.Integer getTaxonomicStatusKey() {
    return taxonomicStatusKey;
  }

  /**
   * Sets the value of the 'taxonomicStatusKey' field.
   * @param value the value to set.
   */
  public void setTaxonomicStatusKey(java.lang.Integer value) {
    this.taxonomicStatusKey = value;
  }

  /**
   * Gets the value of the 'nomenclaturalStatusKey' field.
   */
  public java.util.List<java.lang.Integer> getNomenclaturalStatusKey() {
    return nomenclaturalStatusKey;
  }

  /**
   * Sets the value of the 'nomenclaturalStatusKey' field.
   * @param value the value to set.
   */
  public void setNomenclaturalStatusKey(java.util.List<java.lang.Integer> value) {
    this.nomenclaturalStatusKey = value;
  }

  /**
   * Gets the value of the 'threatStatusKey' field.
   */
  public java.util.List<java.lang.Integer> getThreatStatusKey() {
    return threatStatusKey;
  }

  /**
   * Sets the value of the 'threatStatusKey' field.
   * @param value the value to set.
   */
  public void setThreatStatusKey(java.util.List<java.lang.Integer> value) {
    this.threatStatusKey = value;
  }

  /**
   * Gets the value of the 'rankKey' field.
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
   * Gets the value of the 'habitatKey' field.
   */
  public java.util.List<java.lang.Integer> getHabitatKey() {
    return habitatKey;
  }

  /**
   * Sets the value of the 'habitatKey' field.
   * @param value the value to set.
   */
  public void setHabitatKey(java.util.List<java.lang.Integer> value) {
    this.habitatKey = value;
  }

  /**
   * Gets the value of the 'publishedIn' field.
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
   */
  public java.util.List<java.lang.Integer> getIssues() {
    return issues;
  }

  /**
   * Sets the value of the 'issues' field.
   * @param value the value to set.
   */
  public void setIssues(java.util.List<java.lang.Integer> value) {
    this.issues = value;
  }

  /** Creates a new NameUsageAvro RecordBuilder */
  public static org.gbif.checklistbank.index.model.NameUsageAvro.Builder newBuilder() {
    return new org.gbif.checklistbank.index.model.NameUsageAvro.Builder();
  }
  
  /** Creates a new NameUsageAvro RecordBuilder by copying an existing Builder */
  public static org.gbif.checklistbank.index.model.NameUsageAvro.Builder newBuilder(org.gbif.checklistbank.index.model.NameUsageAvro.Builder other) {
    return new org.gbif.checklistbank.index.model.NameUsageAvro.Builder(other);
  }
  
  /** Creates a new NameUsageAvro RecordBuilder by copying an existing NameUsageAvro instance */
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
    private java.lang.Integer nameType;
    private java.lang.String authorship;
    private java.lang.Integer originKey;
    private java.lang.Integer taxonomicStatusKey;
    private java.util.List<java.lang.Integer> nomenclaturalStatusKey;
    private java.util.List<java.lang.Integer> threatStatusKey;
    private java.lang.Integer rankKey;
    private java.util.List<java.lang.Integer> habitatKey;
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
    private java.util.List<java.lang.Integer> issues;

    /** Creates a new Builder */
    private Builder() {
      super(org.gbif.checklistbank.index.model.NameUsageAvro.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
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
      if (isValidValue(fields()[12], other.nameType)) {
        this.nameType = data().deepCopy(fields()[12].schema(), other.nameType);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.authorship)) {
        this.authorship = data().deepCopy(fields()[13].schema(), other.authorship);
        fieldSetFlags()[13] = true;
      }
      if (isValidValue(fields()[14], other.originKey)) {
        this.originKey = data().deepCopy(fields()[14].schema(), other.originKey);
        fieldSetFlags()[14] = true;
      }
      if (isValidValue(fields()[15], other.taxonomicStatusKey)) {
        this.taxonomicStatusKey = data().deepCopy(fields()[15].schema(), other.taxonomicStatusKey);
        fieldSetFlags()[15] = true;
      }
      if (isValidValue(fields()[16], other.nomenclaturalStatusKey)) {
        this.nomenclaturalStatusKey = data().deepCopy(fields()[16].schema(), other.nomenclaturalStatusKey);
        fieldSetFlags()[16] = true;
      }
      if (isValidValue(fields()[17], other.threatStatusKey)) {
        this.threatStatusKey = data().deepCopy(fields()[17].schema(), other.threatStatusKey);
        fieldSetFlags()[17] = true;
      }
      if (isValidValue(fields()[18], other.rankKey)) {
        this.rankKey = data().deepCopy(fields()[18].schema(), other.rankKey);
        fieldSetFlags()[18] = true;
      }
      if (isValidValue(fields()[19], other.habitatKey)) {
        this.habitatKey = data().deepCopy(fields()[19].schema(), other.habitatKey);
        fieldSetFlags()[19] = true;
      }
      if (isValidValue(fields()[20], other.publishedIn)) {
        this.publishedIn = data().deepCopy(fields()[20].schema(), other.publishedIn);
        fieldSetFlags()[20] = true;
      }
      if (isValidValue(fields()[21], other.accordingTo)) {
        this.accordingTo = data().deepCopy(fields()[21].schema(), other.accordingTo);
        fieldSetFlags()[21] = true;
      }
      if (isValidValue(fields()[22], other.kingdomKey)) {
        this.kingdomKey = data().deepCopy(fields()[22].schema(), other.kingdomKey);
        fieldSetFlags()[22] = true;
      }
      if (isValidValue(fields()[23], other.kingdom)) {
        this.kingdom = data().deepCopy(fields()[23].schema(), other.kingdom);
        fieldSetFlags()[23] = true;
      }
      if (isValidValue(fields()[24], other.phylumKey)) {
        this.phylumKey = data().deepCopy(fields()[24].schema(), other.phylumKey);
        fieldSetFlags()[24] = true;
      }
      if (isValidValue(fields()[25], other.phylum)) {
        this.phylum = data().deepCopy(fields()[25].schema(), other.phylum);
        fieldSetFlags()[25] = true;
      }
      if (isValidValue(fields()[26], other.classKey)) {
        this.classKey = data().deepCopy(fields()[26].schema(), other.classKey);
        fieldSetFlags()[26] = true;
      }
      if (isValidValue(fields()[27], other.clazz)) {
        this.clazz = data().deepCopy(fields()[27].schema(), other.clazz);
        fieldSetFlags()[27] = true;
      }
      if (isValidValue(fields()[28], other.orderKey)) {
        this.orderKey = data().deepCopy(fields()[28].schema(), other.orderKey);
        fieldSetFlags()[28] = true;
      }
      if (isValidValue(fields()[29], other.order)) {
        this.order = data().deepCopy(fields()[29].schema(), other.order);
        fieldSetFlags()[29] = true;
      }
      if (isValidValue(fields()[30], other.familyKey)) {
        this.familyKey = data().deepCopy(fields()[30].schema(), other.familyKey);
        fieldSetFlags()[30] = true;
      }
      if (isValidValue(fields()[31], other.family)) {
        this.family = data().deepCopy(fields()[31].schema(), other.family);
        fieldSetFlags()[31] = true;
      }
      if (isValidValue(fields()[32], other.genusKey)) {
        this.genusKey = data().deepCopy(fields()[32].schema(), other.genusKey);
        fieldSetFlags()[32] = true;
      }
      if (isValidValue(fields()[33], other.genus)) {
        this.genus = data().deepCopy(fields()[33].schema(), other.genus);
        fieldSetFlags()[33] = true;
      }
      if (isValidValue(fields()[34], other.subgenusKey)) {
        this.subgenusKey = data().deepCopy(fields()[34].schema(), other.subgenusKey);
        fieldSetFlags()[34] = true;
      }
      if (isValidValue(fields()[35], other.subgenus)) {
        this.subgenus = data().deepCopy(fields()[35].schema(), other.subgenus);
        fieldSetFlags()[35] = true;
      }
      if (isValidValue(fields()[36], other.speciesKey)) {
        this.speciesKey = data().deepCopy(fields()[36].schema(), other.speciesKey);
        fieldSetFlags()[36] = true;
      }
      if (isValidValue(fields()[37], other.species)) {
        this.species = data().deepCopy(fields()[37].schema(), other.species);
        fieldSetFlags()[37] = true;
      }
      if (isValidValue(fields()[38], other.numDescendants)) {
        this.numDescendants = data().deepCopy(fields()[38].schema(), other.numDescendants);
        fieldSetFlags()[38] = true;
      }
      if (isValidValue(fields()[39], other.sourceId)) {
        this.sourceId = data().deepCopy(fields()[39].schema(), other.sourceId);
        fieldSetFlags()[39] = true;
      }
      if (isValidValue(fields()[40], other.isSynonym)) {
        this.isSynonym = data().deepCopy(fields()[40].schema(), other.isSynonym);
        fieldSetFlags()[40] = true;
      }
      if (isValidValue(fields()[41], other.extinct)) {
        this.extinct = data().deepCopy(fields()[41].schema(), other.extinct);
        fieldSetFlags()[41] = true;
      }
      if (isValidValue(fields()[42], other.description)) {
        this.description = data().deepCopy(fields()[42].schema(), other.description);
        fieldSetFlags()[42] = true;
      }
      if (isValidValue(fields()[43], other.vernacularName)) {
        this.vernacularName = data().deepCopy(fields()[43].schema(), other.vernacularName);
        fieldSetFlags()[43] = true;
      }
      if (isValidValue(fields()[44], other.vernacularLang)) {
        this.vernacularLang = data().deepCopy(fields()[44].schema(), other.vernacularLang);
        fieldSetFlags()[44] = true;
      }
      if (isValidValue(fields()[45], other.vernacularNameLang)) {
        this.vernacularNameLang = data().deepCopy(fields()[45].schema(), other.vernacularNameLang);
        fieldSetFlags()[45] = true;
      }
      if (isValidValue(fields()[46], other.higherTaxonKey)) {
        this.higherTaxonKey = data().deepCopy(fields()[46].schema(), other.higherTaxonKey);
        fieldSetFlags()[46] = true;
      }
      if (isValidValue(fields()[47], other.issues)) {
        this.issues = data().deepCopy(fields()[47].schema(), other.issues);
        fieldSetFlags()[47] = true;
      }
    }
    
    /** Creates a Builder by copying an existing NameUsageAvro instance */
    private Builder(org.gbif.checklistbank.index.model.NameUsageAvro other) {
            super(org.gbif.checklistbank.index.model.NameUsageAvro.SCHEMA$);
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
      if (isValidValue(fields()[12], other.nameType)) {
        this.nameType = data().deepCopy(fields()[12].schema(), other.nameType);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.authorship)) {
        this.authorship = data().deepCopy(fields()[13].schema(), other.authorship);
        fieldSetFlags()[13] = true;
      }
      if (isValidValue(fields()[14], other.originKey)) {
        this.originKey = data().deepCopy(fields()[14].schema(), other.originKey);
        fieldSetFlags()[14] = true;
      }
      if (isValidValue(fields()[15], other.taxonomicStatusKey)) {
        this.taxonomicStatusKey = data().deepCopy(fields()[15].schema(), other.taxonomicStatusKey);
        fieldSetFlags()[15] = true;
      }
      if (isValidValue(fields()[16], other.nomenclaturalStatusKey)) {
        this.nomenclaturalStatusKey = data().deepCopy(fields()[16].schema(), other.nomenclaturalStatusKey);
        fieldSetFlags()[16] = true;
      }
      if (isValidValue(fields()[17], other.threatStatusKey)) {
        this.threatStatusKey = data().deepCopy(fields()[17].schema(), other.threatStatusKey);
        fieldSetFlags()[17] = true;
      }
      if (isValidValue(fields()[18], other.rankKey)) {
        this.rankKey = data().deepCopy(fields()[18].schema(), other.rankKey);
        fieldSetFlags()[18] = true;
      }
      if (isValidValue(fields()[19], other.habitatKey)) {
        this.habitatKey = data().deepCopy(fields()[19].schema(), other.habitatKey);
        fieldSetFlags()[19] = true;
      }
      if (isValidValue(fields()[20], other.publishedIn)) {
        this.publishedIn = data().deepCopy(fields()[20].schema(), other.publishedIn);
        fieldSetFlags()[20] = true;
      }
      if (isValidValue(fields()[21], other.accordingTo)) {
        this.accordingTo = data().deepCopy(fields()[21].schema(), other.accordingTo);
        fieldSetFlags()[21] = true;
      }
      if (isValidValue(fields()[22], other.kingdomKey)) {
        this.kingdomKey = data().deepCopy(fields()[22].schema(), other.kingdomKey);
        fieldSetFlags()[22] = true;
      }
      if (isValidValue(fields()[23], other.kingdom)) {
        this.kingdom = data().deepCopy(fields()[23].schema(), other.kingdom);
        fieldSetFlags()[23] = true;
      }
      if (isValidValue(fields()[24], other.phylumKey)) {
        this.phylumKey = data().deepCopy(fields()[24].schema(), other.phylumKey);
        fieldSetFlags()[24] = true;
      }
      if (isValidValue(fields()[25], other.phylum)) {
        this.phylum = data().deepCopy(fields()[25].schema(), other.phylum);
        fieldSetFlags()[25] = true;
      }
      if (isValidValue(fields()[26], other.classKey)) {
        this.classKey = data().deepCopy(fields()[26].schema(), other.classKey);
        fieldSetFlags()[26] = true;
      }
      if (isValidValue(fields()[27], other.clazz)) {
        this.clazz = data().deepCopy(fields()[27].schema(), other.clazz);
        fieldSetFlags()[27] = true;
      }
      if (isValidValue(fields()[28], other.orderKey)) {
        this.orderKey = data().deepCopy(fields()[28].schema(), other.orderKey);
        fieldSetFlags()[28] = true;
      }
      if (isValidValue(fields()[29], other.order)) {
        this.order = data().deepCopy(fields()[29].schema(), other.order);
        fieldSetFlags()[29] = true;
      }
      if (isValidValue(fields()[30], other.familyKey)) {
        this.familyKey = data().deepCopy(fields()[30].schema(), other.familyKey);
        fieldSetFlags()[30] = true;
      }
      if (isValidValue(fields()[31], other.family)) {
        this.family = data().deepCopy(fields()[31].schema(), other.family);
        fieldSetFlags()[31] = true;
      }
      if (isValidValue(fields()[32], other.genusKey)) {
        this.genusKey = data().deepCopy(fields()[32].schema(), other.genusKey);
        fieldSetFlags()[32] = true;
      }
      if (isValidValue(fields()[33], other.genus)) {
        this.genus = data().deepCopy(fields()[33].schema(), other.genus);
        fieldSetFlags()[33] = true;
      }
      if (isValidValue(fields()[34], other.subgenusKey)) {
        this.subgenusKey = data().deepCopy(fields()[34].schema(), other.subgenusKey);
        fieldSetFlags()[34] = true;
      }
      if (isValidValue(fields()[35], other.subgenus)) {
        this.subgenus = data().deepCopy(fields()[35].schema(), other.subgenus);
        fieldSetFlags()[35] = true;
      }
      if (isValidValue(fields()[36], other.speciesKey)) {
        this.speciesKey = data().deepCopy(fields()[36].schema(), other.speciesKey);
        fieldSetFlags()[36] = true;
      }
      if (isValidValue(fields()[37], other.species)) {
        this.species = data().deepCopy(fields()[37].schema(), other.species);
        fieldSetFlags()[37] = true;
      }
      if (isValidValue(fields()[38], other.numDescendants)) {
        this.numDescendants = data().deepCopy(fields()[38].schema(), other.numDescendants);
        fieldSetFlags()[38] = true;
      }
      if (isValidValue(fields()[39], other.sourceId)) {
        this.sourceId = data().deepCopy(fields()[39].schema(), other.sourceId);
        fieldSetFlags()[39] = true;
      }
      if (isValidValue(fields()[40], other.isSynonym)) {
        this.isSynonym = data().deepCopy(fields()[40].schema(), other.isSynonym);
        fieldSetFlags()[40] = true;
      }
      if (isValidValue(fields()[41], other.extinct)) {
        this.extinct = data().deepCopy(fields()[41].schema(), other.extinct);
        fieldSetFlags()[41] = true;
      }
      if (isValidValue(fields()[42], other.description)) {
        this.description = data().deepCopy(fields()[42].schema(), other.description);
        fieldSetFlags()[42] = true;
      }
      if (isValidValue(fields()[43], other.vernacularName)) {
        this.vernacularName = data().deepCopy(fields()[43].schema(), other.vernacularName);
        fieldSetFlags()[43] = true;
      }
      if (isValidValue(fields()[44], other.vernacularLang)) {
        this.vernacularLang = data().deepCopy(fields()[44].schema(), other.vernacularLang);
        fieldSetFlags()[44] = true;
      }
      if (isValidValue(fields()[45], other.vernacularNameLang)) {
        this.vernacularNameLang = data().deepCopy(fields()[45].schema(), other.vernacularNameLang);
        fieldSetFlags()[45] = true;
      }
      if (isValidValue(fields()[46], other.higherTaxonKey)) {
        this.higherTaxonKey = data().deepCopy(fields()[46].schema(), other.higherTaxonKey);
        fieldSetFlags()[46] = true;
      }
      if (isValidValue(fields()[47], other.issues)) {
        this.issues = data().deepCopy(fields()[47].schema(), other.issues);
        fieldSetFlags()[47] = true;
      }
    }

    /** Gets the value of the 'key' field */
    public java.lang.Integer getKey() {
      return key;
    }
    
    /** Sets the value of the 'key' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setKey(java.lang.Integer value) {
      validate(fields()[0], value);
      this.key = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'key' field has been set */
    public boolean hasKey() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'key' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearKey() {
      key = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'nubKey' field */
    public java.lang.Integer getNubKey() {
      return nubKey;
    }
    
    /** Sets the value of the 'nubKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNubKey(java.lang.Integer value) {
      validate(fields()[1], value);
      this.nubKey = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'nubKey' field has been set */
    public boolean hasNubKey() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'nubKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNubKey() {
      nubKey = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'datasetKey' field */
    public java.lang.String getDatasetKey() {
      return datasetKey;
    }
    
    /** Sets the value of the 'datasetKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setDatasetKey(java.lang.String value) {
      validate(fields()[2], value);
      this.datasetKey = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'datasetKey' field has been set */
    public boolean hasDatasetKey() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'datasetKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearDatasetKey() {
      datasetKey = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'constituentKey' field */
    public java.lang.String getConstituentKey() {
      return constituentKey;
    }
    
    /** Sets the value of the 'constituentKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setConstituentKey(java.lang.String value) {
      validate(fields()[3], value);
      this.constituentKey = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'constituentKey' field has been set */
    public boolean hasConstituentKey() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'constituentKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearConstituentKey() {
      constituentKey = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'parentKey' field */
    public java.lang.Integer getParentKey() {
      return parentKey;
    }
    
    /** Sets the value of the 'parentKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setParentKey(java.lang.Integer value) {
      validate(fields()[4], value);
      this.parentKey = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'parentKey' field has been set */
    public boolean hasParentKey() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'parentKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearParentKey() {
      parentKey = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'parent' field */
    public java.lang.String getParent() {
      return parent;
    }
    
    /** Sets the value of the 'parent' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setParent(java.lang.String value) {
      validate(fields()[5], value);
      this.parent = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'parent' field has been set */
    public boolean hasParent() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'parent' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearParent() {
      parent = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'acceptedKey' field */
    public java.lang.Integer getAcceptedKey() {
      return acceptedKey;
    }
    
    /** Sets the value of the 'acceptedKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAcceptedKey(java.lang.Integer value) {
      validate(fields()[6], value);
      this.acceptedKey = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'acceptedKey' field has been set */
    public boolean hasAcceptedKey() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'acceptedKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAcceptedKey() {
      acceptedKey = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'accepted' field */
    public java.lang.String getAccepted() {
      return accepted;
    }
    
    /** Sets the value of the 'accepted' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAccepted(java.lang.String value) {
      validate(fields()[7], value);
      this.accepted = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'accepted' field has been set */
    public boolean hasAccepted() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'accepted' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAccepted() {
      accepted = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    /** Gets the value of the 'basionymKey' field */
    public java.lang.Integer getBasionymKey() {
      return basionymKey;
    }
    
    /** Sets the value of the 'basionymKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setBasionymKey(java.lang.Integer value) {
      validate(fields()[8], value);
      this.basionymKey = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'basionymKey' field has been set */
    public boolean hasBasionymKey() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'basionymKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearBasionymKey() {
      basionymKey = null;
      fieldSetFlags()[8] = false;
      return this;
    }

    /** Gets the value of the 'basionym' field */
    public java.lang.String getBasionym() {
      return basionym;
    }
    
    /** Sets the value of the 'basionym' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setBasionym(java.lang.String value) {
      validate(fields()[9], value);
      this.basionym = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'basionym' field has been set */
    public boolean hasBasionym() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'basionym' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearBasionym() {
      basionym = null;
      fieldSetFlags()[9] = false;
      return this;
    }

    /** Gets the value of the 'scientificName' field */
    public java.lang.String getScientificName() {
      return scientificName;
    }
    
    /** Sets the value of the 'scientificName' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setScientificName(java.lang.String value) {
      validate(fields()[10], value);
      this.scientificName = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'scientificName' field has been set */
    public boolean hasScientificName() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'scientificName' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearScientificName() {
      scientificName = null;
      fieldSetFlags()[10] = false;
      return this;
    }

    /** Gets the value of the 'canonicalName' field */
    public java.lang.String getCanonicalName() {
      return canonicalName;
    }
    
    /** Sets the value of the 'canonicalName' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setCanonicalName(java.lang.String value) {
      validate(fields()[11], value);
      this.canonicalName = value;
      fieldSetFlags()[11] = true;
      return this; 
    }
    
    /** Checks whether the 'canonicalName' field has been set */
    public boolean hasCanonicalName() {
      return fieldSetFlags()[11];
    }
    
    /** Clears the value of the 'canonicalName' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearCanonicalName() {
      canonicalName = null;
      fieldSetFlags()[11] = false;
      return this;
    }

    /** Gets the value of the 'nameType' field */
    public java.lang.Integer getNameType() {
      return nameType;
    }
    
    /** Sets the value of the 'nameType' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNameType(java.lang.Integer value) {
      validate(fields()[12], value);
      this.nameType = value;
      fieldSetFlags()[12] = true;
      return this; 
    }
    
    /** Checks whether the 'nameType' field has been set */
    public boolean hasNameType() {
      return fieldSetFlags()[12];
    }
    
    /** Clears the value of the 'nameType' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNameType() {
      nameType = null;
      fieldSetFlags()[12] = false;
      return this;
    }

    /** Gets the value of the 'authorship' field */
    public java.lang.String getAuthorship() {
      return authorship;
    }
    
    /** Sets the value of the 'authorship' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAuthorship(java.lang.String value) {
      validate(fields()[13], value);
      this.authorship = value;
      fieldSetFlags()[13] = true;
      return this; 
    }
    
    /** Checks whether the 'authorship' field has been set */
    public boolean hasAuthorship() {
      return fieldSetFlags()[13];
    }
    
    /** Clears the value of the 'authorship' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAuthorship() {
      authorship = null;
      fieldSetFlags()[13] = false;
      return this;
    }

    /** Gets the value of the 'originKey' field */
    public java.lang.Integer getOriginKey() {
      return originKey;
    }
    
    /** Sets the value of the 'originKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setOriginKey(java.lang.Integer value) {
      validate(fields()[14], value);
      this.originKey = value;
      fieldSetFlags()[14] = true;
      return this; 
    }
    
    /** Checks whether the 'originKey' field has been set */
    public boolean hasOriginKey() {
      return fieldSetFlags()[14];
    }
    
    /** Clears the value of the 'originKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearOriginKey() {
      originKey = null;
      fieldSetFlags()[14] = false;
      return this;
    }

    /** Gets the value of the 'taxonomicStatusKey' field */
    public java.lang.Integer getTaxonomicStatusKey() {
      return taxonomicStatusKey;
    }
    
    /** Sets the value of the 'taxonomicStatusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setTaxonomicStatusKey(java.lang.Integer value) {
      validate(fields()[15], value);
      this.taxonomicStatusKey = value;
      fieldSetFlags()[15] = true;
      return this; 
    }
    
    /** Checks whether the 'taxonomicStatusKey' field has been set */
    public boolean hasTaxonomicStatusKey() {
      return fieldSetFlags()[15];
    }
    
    /** Clears the value of the 'taxonomicStatusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearTaxonomicStatusKey() {
      taxonomicStatusKey = null;
      fieldSetFlags()[15] = false;
      return this;
    }

    /** Gets the value of the 'nomenclaturalStatusKey' field */
    public java.util.List<java.lang.Integer> getNomenclaturalStatusKey() {
      return nomenclaturalStatusKey;
    }
    
    /** Sets the value of the 'nomenclaturalStatusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNomenclaturalStatusKey(java.util.List<java.lang.Integer> value) {
      validate(fields()[16], value);
      this.nomenclaturalStatusKey = value;
      fieldSetFlags()[16] = true;
      return this; 
    }
    
    /** Checks whether the 'nomenclaturalStatusKey' field has been set */
    public boolean hasNomenclaturalStatusKey() {
      return fieldSetFlags()[16];
    }
    
    /** Clears the value of the 'nomenclaturalStatusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNomenclaturalStatusKey() {
      nomenclaturalStatusKey = null;
      fieldSetFlags()[16] = false;
      return this;
    }

    /** Gets the value of the 'threatStatusKey' field */
    public java.util.List<java.lang.Integer> getThreatStatusKey() {
      return threatStatusKey;
    }
    
    /** Sets the value of the 'threatStatusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setThreatStatusKey(java.util.List<java.lang.Integer> value) {
      validate(fields()[17], value);
      this.threatStatusKey = value;
      fieldSetFlags()[17] = true;
      return this; 
    }
    
    /** Checks whether the 'threatStatusKey' field has been set */
    public boolean hasThreatStatusKey() {
      return fieldSetFlags()[17];
    }
    
    /** Clears the value of the 'threatStatusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearThreatStatusKey() {
      threatStatusKey = null;
      fieldSetFlags()[17] = false;
      return this;
    }

    /** Gets the value of the 'rankKey' field */
    public java.lang.Integer getRankKey() {
      return rankKey;
    }
    
    /** Sets the value of the 'rankKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setRankKey(java.lang.Integer value) {
      validate(fields()[18], value);
      this.rankKey = value;
      fieldSetFlags()[18] = true;
      return this; 
    }
    
    /** Checks whether the 'rankKey' field has been set */
    public boolean hasRankKey() {
      return fieldSetFlags()[18];
    }
    
    /** Clears the value of the 'rankKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearRankKey() {
      rankKey = null;
      fieldSetFlags()[18] = false;
      return this;
    }

    /** Gets the value of the 'habitatKey' field */
    public java.util.List<java.lang.Integer> getHabitatKey() {
      return habitatKey;
    }
    
    /** Sets the value of the 'habitatKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setHabitatKey(java.util.List<java.lang.Integer> value) {
      validate(fields()[19], value);
      this.habitatKey = value;
      fieldSetFlags()[19] = true;
      return this; 
    }
    
    /** Checks whether the 'habitatKey' field has been set */
    public boolean hasHabitatKey() {
      return fieldSetFlags()[19];
    }
    
    /** Clears the value of the 'habitatKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearHabitatKey() {
      habitatKey = null;
      fieldSetFlags()[19] = false;
      return this;
    }

    /** Gets the value of the 'publishedIn' field */
    public java.lang.String getPublishedIn() {
      return publishedIn;
    }
    
    /** Sets the value of the 'publishedIn' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setPublishedIn(java.lang.String value) {
      validate(fields()[20], value);
      this.publishedIn = value;
      fieldSetFlags()[20] = true;
      return this; 
    }
    
    /** Checks whether the 'publishedIn' field has been set */
    public boolean hasPublishedIn() {
      return fieldSetFlags()[20];
    }
    
    /** Clears the value of the 'publishedIn' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearPublishedIn() {
      publishedIn = null;
      fieldSetFlags()[20] = false;
      return this;
    }

    /** Gets the value of the 'accordingTo' field */
    public java.lang.String getAccordingTo() {
      return accordingTo;
    }
    
    /** Sets the value of the 'accordingTo' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setAccordingTo(java.lang.String value) {
      validate(fields()[21], value);
      this.accordingTo = value;
      fieldSetFlags()[21] = true;
      return this; 
    }
    
    /** Checks whether the 'accordingTo' field has been set */
    public boolean hasAccordingTo() {
      return fieldSetFlags()[21];
    }
    
    /** Clears the value of the 'accordingTo' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearAccordingTo() {
      accordingTo = null;
      fieldSetFlags()[21] = false;
      return this;
    }

    /** Gets the value of the 'kingdomKey' field */
    public java.lang.Integer getKingdomKey() {
      return kingdomKey;
    }
    
    /** Sets the value of the 'kingdomKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setKingdomKey(java.lang.Integer value) {
      validate(fields()[22], value);
      this.kingdomKey = value;
      fieldSetFlags()[22] = true;
      return this; 
    }
    
    /** Checks whether the 'kingdomKey' field has been set */
    public boolean hasKingdomKey() {
      return fieldSetFlags()[22];
    }
    
    /** Clears the value of the 'kingdomKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearKingdomKey() {
      kingdomKey = null;
      fieldSetFlags()[22] = false;
      return this;
    }

    /** Gets the value of the 'kingdom' field */
    public java.lang.String getKingdom() {
      return kingdom;
    }
    
    /** Sets the value of the 'kingdom' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setKingdom(java.lang.String value) {
      validate(fields()[23], value);
      this.kingdom = value;
      fieldSetFlags()[23] = true;
      return this; 
    }
    
    /** Checks whether the 'kingdom' field has been set */
    public boolean hasKingdom() {
      return fieldSetFlags()[23];
    }
    
    /** Clears the value of the 'kingdom' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearKingdom() {
      kingdom = null;
      fieldSetFlags()[23] = false;
      return this;
    }

    /** Gets the value of the 'phylumKey' field */
    public java.lang.Integer getPhylumKey() {
      return phylumKey;
    }
    
    /** Sets the value of the 'phylumKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setPhylumKey(java.lang.Integer value) {
      validate(fields()[24], value);
      this.phylumKey = value;
      fieldSetFlags()[24] = true;
      return this; 
    }
    
    /** Checks whether the 'phylumKey' field has been set */
    public boolean hasPhylumKey() {
      return fieldSetFlags()[24];
    }
    
    /** Clears the value of the 'phylumKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearPhylumKey() {
      phylumKey = null;
      fieldSetFlags()[24] = false;
      return this;
    }

    /** Gets the value of the 'phylum' field */
    public java.lang.String getPhylum() {
      return phylum;
    }
    
    /** Sets the value of the 'phylum' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setPhylum(java.lang.String value) {
      validate(fields()[25], value);
      this.phylum = value;
      fieldSetFlags()[25] = true;
      return this; 
    }
    
    /** Checks whether the 'phylum' field has been set */
    public boolean hasPhylum() {
      return fieldSetFlags()[25];
    }
    
    /** Clears the value of the 'phylum' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearPhylum() {
      phylum = null;
      fieldSetFlags()[25] = false;
      return this;
    }

    /** Gets the value of the 'classKey' field */
    public java.lang.Integer getClassKey() {
      return classKey;
    }
    
    /** Sets the value of the 'classKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setClassKey(java.lang.Integer value) {
      validate(fields()[26], value);
      this.classKey = value;
      fieldSetFlags()[26] = true;
      return this; 
    }
    
    /** Checks whether the 'classKey' field has been set */
    public boolean hasClassKey() {
      return fieldSetFlags()[26];
    }
    
    /** Clears the value of the 'classKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearClassKey() {
      classKey = null;
      fieldSetFlags()[26] = false;
      return this;
    }

    /** Gets the value of the 'clazz' field */
    public java.lang.String getClazz() {
      return clazz;
    }
    
    /** Sets the value of the 'clazz' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setClazz(java.lang.String value) {
      validate(fields()[27], value);
      this.clazz = value;
      fieldSetFlags()[27] = true;
      return this; 
    }
    
    /** Checks whether the 'clazz' field has been set */
    public boolean hasClazz() {
      return fieldSetFlags()[27];
    }
    
    /** Clears the value of the 'clazz' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearClazz() {
      clazz = null;
      fieldSetFlags()[27] = false;
      return this;
    }

    /** Gets the value of the 'orderKey' field */
    public java.lang.Integer getOrderKey() {
      return orderKey;
    }
    
    /** Sets the value of the 'orderKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setOrderKey(java.lang.Integer value) {
      validate(fields()[28], value);
      this.orderKey = value;
      fieldSetFlags()[28] = true;
      return this; 
    }
    
    /** Checks whether the 'orderKey' field has been set */
    public boolean hasOrderKey() {
      return fieldSetFlags()[28];
    }
    
    /** Clears the value of the 'orderKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearOrderKey() {
      orderKey = null;
      fieldSetFlags()[28] = false;
      return this;
    }

    /** Gets the value of the 'order' field */
    public java.lang.String getOrder() {
      return order;
    }
    
    /** Sets the value of the 'order' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setOrder(java.lang.String value) {
      validate(fields()[29], value);
      this.order = value;
      fieldSetFlags()[29] = true;
      return this; 
    }
    
    /** Checks whether the 'order' field has been set */
    public boolean hasOrder() {
      return fieldSetFlags()[29];
    }
    
    /** Clears the value of the 'order' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearOrder() {
      order = null;
      fieldSetFlags()[29] = false;
      return this;
    }

    /** Gets the value of the 'familyKey' field */
    public java.lang.Integer getFamilyKey() {
      return familyKey;
    }
    
    /** Sets the value of the 'familyKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setFamilyKey(java.lang.Integer value) {
      validate(fields()[30], value);
      this.familyKey = value;
      fieldSetFlags()[30] = true;
      return this; 
    }
    
    /** Checks whether the 'familyKey' field has been set */
    public boolean hasFamilyKey() {
      return fieldSetFlags()[30];
    }
    
    /** Clears the value of the 'familyKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearFamilyKey() {
      familyKey = null;
      fieldSetFlags()[30] = false;
      return this;
    }

    /** Gets the value of the 'family' field */
    public java.lang.String getFamily() {
      return family;
    }
    
    /** Sets the value of the 'family' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setFamily(java.lang.String value) {
      validate(fields()[31], value);
      this.family = value;
      fieldSetFlags()[31] = true;
      return this; 
    }
    
    /** Checks whether the 'family' field has been set */
    public boolean hasFamily() {
      return fieldSetFlags()[31];
    }
    
    /** Clears the value of the 'family' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearFamily() {
      family = null;
      fieldSetFlags()[31] = false;
      return this;
    }

    /** Gets the value of the 'genusKey' field */
    public java.lang.Integer getGenusKey() {
      return genusKey;
    }
    
    /** Sets the value of the 'genusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setGenusKey(java.lang.Integer value) {
      validate(fields()[32], value);
      this.genusKey = value;
      fieldSetFlags()[32] = true;
      return this; 
    }
    
    /** Checks whether the 'genusKey' field has been set */
    public boolean hasGenusKey() {
      return fieldSetFlags()[32];
    }
    
    /** Clears the value of the 'genusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearGenusKey() {
      genusKey = null;
      fieldSetFlags()[32] = false;
      return this;
    }

    /** Gets the value of the 'genus' field */
    public java.lang.String getGenus() {
      return genus;
    }
    
    /** Sets the value of the 'genus' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setGenus(java.lang.String value) {
      validate(fields()[33], value);
      this.genus = value;
      fieldSetFlags()[33] = true;
      return this; 
    }
    
    /** Checks whether the 'genus' field has been set */
    public boolean hasGenus() {
      return fieldSetFlags()[33];
    }
    
    /** Clears the value of the 'genus' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearGenus() {
      genus = null;
      fieldSetFlags()[33] = false;
      return this;
    }

    /** Gets the value of the 'subgenusKey' field */
    public java.lang.Integer getSubgenusKey() {
      return subgenusKey;
    }
    
    /** Sets the value of the 'subgenusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSubgenusKey(java.lang.Integer value) {
      validate(fields()[34], value);
      this.subgenusKey = value;
      fieldSetFlags()[34] = true;
      return this; 
    }
    
    /** Checks whether the 'subgenusKey' field has been set */
    public boolean hasSubgenusKey() {
      return fieldSetFlags()[34];
    }
    
    /** Clears the value of the 'subgenusKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSubgenusKey() {
      subgenusKey = null;
      fieldSetFlags()[34] = false;
      return this;
    }

    /** Gets the value of the 'subgenus' field */
    public java.lang.String getSubgenus() {
      return subgenus;
    }
    
    /** Sets the value of the 'subgenus' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSubgenus(java.lang.String value) {
      validate(fields()[35], value);
      this.subgenus = value;
      fieldSetFlags()[35] = true;
      return this; 
    }
    
    /** Checks whether the 'subgenus' field has been set */
    public boolean hasSubgenus() {
      return fieldSetFlags()[35];
    }
    
    /** Clears the value of the 'subgenus' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSubgenus() {
      subgenus = null;
      fieldSetFlags()[35] = false;
      return this;
    }

    /** Gets the value of the 'speciesKey' field */
    public java.lang.Integer getSpeciesKey() {
      return speciesKey;
    }
    
    /** Sets the value of the 'speciesKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSpeciesKey(java.lang.Integer value) {
      validate(fields()[36], value);
      this.speciesKey = value;
      fieldSetFlags()[36] = true;
      return this; 
    }
    
    /** Checks whether the 'speciesKey' field has been set */
    public boolean hasSpeciesKey() {
      return fieldSetFlags()[36];
    }
    
    /** Clears the value of the 'speciesKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSpeciesKey() {
      speciesKey = null;
      fieldSetFlags()[36] = false;
      return this;
    }

    /** Gets the value of the 'species' field */
    public java.lang.String getSpecies() {
      return species;
    }
    
    /** Sets the value of the 'species' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSpecies(java.lang.String value) {
      validate(fields()[37], value);
      this.species = value;
      fieldSetFlags()[37] = true;
      return this; 
    }
    
    /** Checks whether the 'species' field has been set */
    public boolean hasSpecies() {
      return fieldSetFlags()[37];
    }
    
    /** Clears the value of the 'species' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSpecies() {
      species = null;
      fieldSetFlags()[37] = false;
      return this;
    }

    /** Gets the value of the 'numDescendants' field */
    public java.lang.Integer getNumDescendants() {
      return numDescendants;
    }
    
    /** Sets the value of the 'numDescendants' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setNumDescendants(java.lang.Integer value) {
      validate(fields()[38], value);
      this.numDescendants = value;
      fieldSetFlags()[38] = true;
      return this; 
    }
    
    /** Checks whether the 'numDescendants' field has been set */
    public boolean hasNumDescendants() {
      return fieldSetFlags()[38];
    }
    
    /** Clears the value of the 'numDescendants' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearNumDescendants() {
      numDescendants = null;
      fieldSetFlags()[38] = false;
      return this;
    }

    /** Gets the value of the 'sourceId' field */
    public java.lang.String getSourceId() {
      return sourceId;
    }
    
    /** Sets the value of the 'sourceId' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setSourceId(java.lang.String value) {
      validate(fields()[39], value);
      this.sourceId = value;
      fieldSetFlags()[39] = true;
      return this; 
    }
    
    /** Checks whether the 'sourceId' field has been set */
    public boolean hasSourceId() {
      return fieldSetFlags()[39];
    }
    
    /** Clears the value of the 'sourceId' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearSourceId() {
      sourceId = null;
      fieldSetFlags()[39] = false;
      return this;
    }

    /** Gets the value of the 'isSynonym' field */
    public java.lang.Boolean getIsSynonym() {
      return isSynonym;
    }
    
    /** Sets the value of the 'isSynonym' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setIsSynonym(java.lang.Boolean value) {
      validate(fields()[40], value);
      this.isSynonym = value;
      fieldSetFlags()[40] = true;
      return this; 
    }
    
    /** Checks whether the 'isSynonym' field has been set */
    public boolean hasIsSynonym() {
      return fieldSetFlags()[40];
    }
    
    /** Clears the value of the 'isSynonym' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearIsSynonym() {
      isSynonym = null;
      fieldSetFlags()[40] = false;
      return this;
    }

    /** Gets the value of the 'extinct' field */
    public java.lang.Boolean getExtinct() {
      return extinct;
    }
    
    /** Sets the value of the 'extinct' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setExtinct(java.lang.Boolean value) {
      validate(fields()[41], value);
      this.extinct = value;
      fieldSetFlags()[41] = true;
      return this; 
    }
    
    /** Checks whether the 'extinct' field has been set */
    public boolean hasExtinct() {
      return fieldSetFlags()[41];
    }
    
    /** Clears the value of the 'extinct' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearExtinct() {
      extinct = null;
      fieldSetFlags()[41] = false;
      return this;
    }

    /** Gets the value of the 'description' field */
    public java.util.List<java.lang.String> getDescription() {
      return description;
    }
    
    /** Sets the value of the 'description' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setDescription(java.util.List<java.lang.String> value) {
      validate(fields()[42], value);
      this.description = value;
      fieldSetFlags()[42] = true;
      return this; 
    }
    
    /** Checks whether the 'description' field has been set */
    public boolean hasDescription() {
      return fieldSetFlags()[42];
    }
    
    /** Clears the value of the 'description' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearDescription() {
      description = null;
      fieldSetFlags()[42] = false;
      return this;
    }

    /** Gets the value of the 'vernacularName' field */
    public java.util.List<java.lang.String> getVernacularName() {
      return vernacularName;
    }
    
    /** Sets the value of the 'vernacularName' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setVernacularName(java.util.List<java.lang.String> value) {
      validate(fields()[43], value);
      this.vernacularName = value;
      fieldSetFlags()[43] = true;
      return this; 
    }
    
    /** Checks whether the 'vernacularName' field has been set */
    public boolean hasVernacularName() {
      return fieldSetFlags()[43];
    }
    
    /** Clears the value of the 'vernacularName' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearVernacularName() {
      vernacularName = null;
      fieldSetFlags()[43] = false;
      return this;
    }

    /** Gets the value of the 'vernacularLang' field */
    public java.util.List<java.lang.String> getVernacularLang() {
      return vernacularLang;
    }
    
    /** Sets the value of the 'vernacularLang' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setVernacularLang(java.util.List<java.lang.String> value) {
      validate(fields()[44], value);
      this.vernacularLang = value;
      fieldSetFlags()[44] = true;
      return this; 
    }
    
    /** Checks whether the 'vernacularLang' field has been set */
    public boolean hasVernacularLang() {
      return fieldSetFlags()[44];
    }
    
    /** Clears the value of the 'vernacularLang' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearVernacularLang() {
      vernacularLang = null;
      fieldSetFlags()[44] = false;
      return this;
    }

    /** Gets the value of the 'vernacularNameLang' field */
    public java.util.List<java.lang.String> getVernacularNameLang() {
      return vernacularNameLang;
    }
    
    /** Sets the value of the 'vernacularNameLang' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setVernacularNameLang(java.util.List<java.lang.String> value) {
      validate(fields()[45], value);
      this.vernacularNameLang = value;
      fieldSetFlags()[45] = true;
      return this; 
    }
    
    /** Checks whether the 'vernacularNameLang' field has been set */
    public boolean hasVernacularNameLang() {
      return fieldSetFlags()[45];
    }
    
    /** Clears the value of the 'vernacularNameLang' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearVernacularNameLang() {
      vernacularNameLang = null;
      fieldSetFlags()[45] = false;
      return this;
    }

    /** Gets the value of the 'higherTaxonKey' field */
    public java.util.List<java.lang.Integer> getHigherTaxonKey() {
      return higherTaxonKey;
    }
    
    /** Sets the value of the 'higherTaxonKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setHigherTaxonKey(java.util.List<java.lang.Integer> value) {
      validate(fields()[46], value);
      this.higherTaxonKey = value;
      fieldSetFlags()[46] = true;
      return this; 
    }
    
    /** Checks whether the 'higherTaxonKey' field has been set */
    public boolean hasHigherTaxonKey() {
      return fieldSetFlags()[46];
    }
    
    /** Clears the value of the 'higherTaxonKey' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearHigherTaxonKey() {
      higherTaxonKey = null;
      fieldSetFlags()[46] = false;
      return this;
    }

    /** Gets the value of the 'issues' field */
    public java.util.List<java.lang.Integer> getIssues() {
      return issues;
    }
    
    /** Sets the value of the 'issues' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder setIssues(java.util.List<java.lang.Integer> value) {
      validate(fields()[47], value);
      this.issues = value;
      fieldSetFlags()[47] = true;
      return this; 
    }
    
    /** Checks whether the 'issues' field has been set */
    public boolean hasIssues() {
      return fieldSetFlags()[47];
    }
    
    /** Clears the value of the 'issues' field */
    public org.gbif.checklistbank.index.model.NameUsageAvro.Builder clearIssues() {
      issues = null;
      fieldSetFlags()[47] = false;
      return this;
    }

    @Override
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
        record.nameType = fieldSetFlags()[12] ? this.nameType : (java.lang.Integer) defaultValue(fields()[12]);
        record.authorship = fieldSetFlags()[13] ? this.authorship : (java.lang.String) defaultValue(fields()[13]);
        record.originKey = fieldSetFlags()[14] ? this.originKey : (java.lang.Integer) defaultValue(fields()[14]);
        record.taxonomicStatusKey = fieldSetFlags()[15] ? this.taxonomicStatusKey : (java.lang.Integer) defaultValue(fields()[15]);
        record.nomenclaturalStatusKey = fieldSetFlags()[16] ? this.nomenclaturalStatusKey : (java.util.List<java.lang.Integer>) defaultValue(fields()[16]);
        record.threatStatusKey = fieldSetFlags()[17] ? this.threatStatusKey : (java.util.List<java.lang.Integer>) defaultValue(fields()[17]);
        record.rankKey = fieldSetFlags()[18] ? this.rankKey : (java.lang.Integer) defaultValue(fields()[18]);
        record.habitatKey = fieldSetFlags()[19] ? this.habitatKey : (java.util.List<java.lang.Integer>) defaultValue(fields()[19]);
        record.publishedIn = fieldSetFlags()[20] ? this.publishedIn : (java.lang.String) defaultValue(fields()[20]);
        record.accordingTo = fieldSetFlags()[21] ? this.accordingTo : (java.lang.String) defaultValue(fields()[21]);
        record.kingdomKey = fieldSetFlags()[22] ? this.kingdomKey : (java.lang.Integer) defaultValue(fields()[22]);
        record.kingdom = fieldSetFlags()[23] ? this.kingdom : (java.lang.String) defaultValue(fields()[23]);
        record.phylumKey = fieldSetFlags()[24] ? this.phylumKey : (java.lang.Integer) defaultValue(fields()[24]);
        record.phylum = fieldSetFlags()[25] ? this.phylum : (java.lang.String) defaultValue(fields()[25]);
        record.classKey = fieldSetFlags()[26] ? this.classKey : (java.lang.Integer) defaultValue(fields()[26]);
        record.clazz = fieldSetFlags()[27] ? this.clazz : (java.lang.String) defaultValue(fields()[27]);
        record.orderKey = fieldSetFlags()[28] ? this.orderKey : (java.lang.Integer) defaultValue(fields()[28]);
        record.order = fieldSetFlags()[29] ? this.order : (java.lang.String) defaultValue(fields()[29]);
        record.familyKey = fieldSetFlags()[30] ? this.familyKey : (java.lang.Integer) defaultValue(fields()[30]);
        record.family = fieldSetFlags()[31] ? this.family : (java.lang.String) defaultValue(fields()[31]);
        record.genusKey = fieldSetFlags()[32] ? this.genusKey : (java.lang.Integer) defaultValue(fields()[32]);
        record.genus = fieldSetFlags()[33] ? this.genus : (java.lang.String) defaultValue(fields()[33]);
        record.subgenusKey = fieldSetFlags()[34] ? this.subgenusKey : (java.lang.Integer) defaultValue(fields()[34]);
        record.subgenus = fieldSetFlags()[35] ? this.subgenus : (java.lang.String) defaultValue(fields()[35]);
        record.speciesKey = fieldSetFlags()[36] ? this.speciesKey : (java.lang.Integer) defaultValue(fields()[36]);
        record.species = fieldSetFlags()[37] ? this.species : (java.lang.String) defaultValue(fields()[37]);
        record.numDescendants = fieldSetFlags()[38] ? this.numDescendants : (java.lang.Integer) defaultValue(fields()[38]);
        record.sourceId = fieldSetFlags()[39] ? this.sourceId : (java.lang.String) defaultValue(fields()[39]);
        record.isSynonym = fieldSetFlags()[40] ? this.isSynonym : (java.lang.Boolean) defaultValue(fields()[40]);
        record.extinct = fieldSetFlags()[41] ? this.extinct : (java.lang.Boolean) defaultValue(fields()[41]);
        record.description = fieldSetFlags()[42] ? this.description : (java.util.List<java.lang.String>) defaultValue(fields()[42]);
        record.vernacularName = fieldSetFlags()[43] ? this.vernacularName : (java.util.List<java.lang.String>) defaultValue(fields()[43]);
        record.vernacularLang = fieldSetFlags()[44] ? this.vernacularLang : (java.util.List<java.lang.String>) defaultValue(fields()[44]);
        record.vernacularNameLang = fieldSetFlags()[45] ? this.vernacularNameLang : (java.util.List<java.lang.String>) defaultValue(fields()[45]);
        record.higherTaxonKey = fieldSetFlags()[46] ? this.higherTaxonKey : (java.util.List<java.lang.Integer>) defaultValue(fields()[46]);
        record.issues = fieldSetFlags()[47] ? this.issues : (java.util.List<java.lang.Integer>) defaultValue(fields()[47]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
