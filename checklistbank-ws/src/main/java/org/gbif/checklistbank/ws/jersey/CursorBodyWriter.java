package org.gbif.checklistbank.ws.jersey;

import org.apache.ibatis.cursor.Cursor;
import org.codehaus.jackson.map.ObjectMapper;

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

/**
 * Writer that generates an JSON array based on any postgres backed cursor
 * and streams the results to the output using the main jackson API mapper.
 */
@Produces(MediaType.APPLICATION_JSON)
@Provider
public class CursorBodyWriter implements MessageBodyWriter<Cursor<?>> {

  @Override
  public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
    return Cursor.class.isAssignableFrom(type);
  }

  @Override
  public long getSize(Cursor<?> cursor, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
    return 0;
  }

  @Override
  public void writeTo(Cursor<?> c, Class<?> type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap<String, Object> mm, OutputStream out) throws IOException, WebApplicationException {
    try (StreamBodyWriter.JsonArrayConsumer consumer = new StreamBodyWriter.JsonArrayConsumer(out)){
      c.forEach(consumer);
    }
  }
}