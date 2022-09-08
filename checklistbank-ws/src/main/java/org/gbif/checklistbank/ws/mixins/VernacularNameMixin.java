package org.gbif.checklistbank.ws.mixins;

import org.gbif.api.vocabulary.Language;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public abstract class VernacularNameMixin {

  @JsonSerialize(using = LanguageSerializers.NameSerializer.class)
  @JsonDeserialize(using = LanguageSerializers.LenientDeserializer.class)
  private Language language;

}
