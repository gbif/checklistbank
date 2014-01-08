package org.gbif.checklistbank.index.model;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.common.search.model.HighlightableList;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * A facade to a description stored in a serialized format in solr that allows to support solr highlighting on the
 * description field of the complex Description class.
 */
public class HighlightableDescription implements HighlightableList {

  private final List<Description> descriptions;

  public HighlightableDescription(List<Description> descriptions) {
    this.descriptions = descriptions;
  }

  @Override
  public List<String> getValues() {
    List<String> values = Lists.newArrayList();
    for (Description d : descriptions) {
      values.add(NameUsageDocConverter.serializeDescription(d));
    }
    return values;
  }

  @Override
  public void replaceValue(int index, String newValue) {
    // watch out, we get a marked up serialized string here!
    Description d = NameUsageDocConverter.deserializeDescription(newValue);
    descriptions.get(index).setDescription(d.getDescription());
  }
}
