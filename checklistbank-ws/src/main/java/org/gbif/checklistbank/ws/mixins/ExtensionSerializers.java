package org.gbif.checklistbank.ws.mixins;

import org.gbif.api.vocabulary.Extension;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ExtensionSerializers {

  private ExtensionSerializers() {}

  public static class ExtensionKeySerializer extends JsonSerializer<Extension> {
    public ExtensionKeySerializer() {}

    @Override
    public void serialize(Extension value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeFieldName(value.name());
    }
  }

  public static class ExtensionKeyDeserializer extends KeyDeserializer {
    public ExtensionKeyDeserializer() {}

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
      return Extension.valueOf(key);
    }
  }
}
