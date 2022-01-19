package org.gbif.checklistbank.ws.mixins;

import org.gbif.api.vocabulary.Language;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public interface TableOfContentsMixin {

  @JsonSerialize(keyUsing = LanguageSerializers.NameTocKeySerializer.class)
  Map<Language, Map<String, List<Integer>>> getToc();
}
