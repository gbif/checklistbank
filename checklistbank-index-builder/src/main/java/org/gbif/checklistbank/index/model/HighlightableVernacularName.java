package org.gbif.checklistbank.index.model;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.common.search.model.HighlightableList;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * A facade to a vernacular name stored in a serialized format in solr that allows to support solr highlighting on the
 * vernacularName field of the complex VernacularName class.
 */
public class HighlightableVernacularName implements HighlightableList {

  private final List<VernacularName> names;

  public HighlightableVernacularName(List<VernacularName> names) {
    this.names = names;
  }

  @Override
  public List<String> getValues() {
    List<String> values = Lists.newArrayList();
    for (VernacularName d : names) {
      values.add(NameUsageDocConverter.serializeVernacularName(d));
    }
    return values;
  }

  @Override
  public void replaceValue(int index, String newValue) {
    // watch out, we get a marked up serialized string here!
    VernacularName v = NameUsageDocConverter.deserializeVernacularName(newValue);
    names.get(index).setVernacularName(v.getVernacularName());
  }
}
