package org.gbif.checklistbank.ws.mixins;

import org.gbif.api.vocabulary.Extension;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(
    using = ExtensionMixin.ExtensionSerializer.class,
    keyUsing = ExtensionMixin.ExtensionSerializer.class)
@JsonDeserialize(
    using = ExtensionMixin.ExtensionDeserializer.class,
    keyUsing = ExtensionMixin.ExtensionKeyDeserializer.class)
public interface ExtensionMixin {

  class ExtensionDeserializer extends JsonDeserializer<Extension> {
    public ExtensionDeserializer() {}

    @Override
    public Extension deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
        return Extension.valueOf(jp.getText());
      } else {
        throw JsonMappingException.from(jp, "Expected JSON String");
      }
    }
  }

  class ExtensionSerializer extends JsonSerializer<Extension> {
    public ExtensionSerializer() {}

    @Override
    public void serialize(Extension value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeFieldName(value.name());
    }
  }

  class ExtensionKeyDeserializer extends KeyDeserializer {
    public ExtensionKeyDeserializer() {}

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
      return Extension.valueOf(key);
    }
  }
}
