package org.gbif.checklistbank.ws.jersey;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Writer that generates an JSON array based on any object stream
 * and streams the results to the output using the main jackson API mapper.
 */
@Produces(MediaType.APPLICATION_JSON)
@Provider
public class StreamBodyWriter implements MessageBodyWriter<Stream<?>> {
  private static final Logger LOG = LoggerFactory.getLogger(StreamBodyWriter.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
    return Stream.class.isAssignableFrom(type);
  }

  @Override
  public long getSize(Stream<?> stream, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
    return 0;
  }

  @Override
  public void writeTo(Stream<?> stream, Class<?> type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap<String, Object> mm, OutputStream out) throws IOException, WebApplicationException {
    try (JsonArrayConsumer consumer = new JsonArrayConsumer(out)){
      stream.forEach(consumer);
    }
  }

  public static class JsonArrayConsumer implements Consumer<Object>, AutoCloseable {
    private final OutputStream out;
    private boolean first = true;

    JsonArrayConsumer(OutputStream out) {
      this.out = out;
      try {
        out.write('[');
      } catch (IOException e) {
        LOG.error("Failed to write to output steam", e);
        throw new RuntimeException(e);
      }
    }

    @Override
    public void accept(Object o) {
      try {
        if (first) {
          first = false;
        } else {
          out.write(',');
          out.write('\n');
        }
        MAPPER.writeValue(out, o);

      } catch (IOException e) {
        LOG.error("Failed to consume object {}", o, e);
      }
    }

    @Override
    public void close() {
      try {
        out.write(']');
      } catch (IOException e) {
        LOG.error("Failed to write to output steam", e);
        throw new RuntimeException(e);
      }
    }
  }
}