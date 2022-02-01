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
