package org.gbif.checklistbank.ws.mixins;

import org.gbif.api.vocabulary.Extension;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public abstract class DatasetMetricsMixin {

  @JsonSerialize(keyUsing = ExtensionSerializers.ExtensionKeySerializer.class)
  @JsonDeserialize(keyUsing = ExtensionSerializers.ExtensionKeyDeserializer.class)
  private Map<Extension, Integer> countExtRecordsByExtension = new HashMap();
}
