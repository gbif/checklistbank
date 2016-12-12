package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.checklistbank.ws.util.LineReader;
import org.gbif.ws.util.ExtraMediaTypes;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sun.jersey.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The scientific name parser exposed in the API.
 */
@Path("/parser/name")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class NameParserResource {

  private static final Logger LOG = LoggerFactory.getLogger(NameParserResource.class);
  private static final Splitter NEW_LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings().trimResults();
  private final NameParser parser;

  @Inject
  public NameParserResource(NameParser parser) {
    this.parser = parser;
  }

  /**
   * Parsing names as GET query parameters.
   */
  @GET
  public List<ParsedName> parseGet(@QueryParam("name") List<String> names) {
    return parse(names.iterator());
  }

  /**
   * Parsing names as a json array.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public List<ParsedName> parseJson(List<String> names) {
    return parse(names.iterator());
  }

  /**
   * Parsing names by uploading a plain UTF-8 text file using one line per scientific name.
   * <pre>
   * curl -F names=@scientific_names.txt http://apidev.gbif.org/parser/name
   * </pre>
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public List<ParsedName> parseFile(@FormDataParam("names") InputStream namesFile) throws UnsupportedEncodingException {
    if (namesFile == null) {
      LOG.debug("No names file uploaded");
      return Lists.newArrayList();
    }
    LineReader iter = new LineReader(namesFile, Charset.forName("UTF8"));
    return parse(iter.iterator());
  }


  /**
   * Parsing names by posting plain text content using one line per scientific name.
   * Make sure to preserve new lines (\n) in the posted data, for example use --data-binary with curl:
   * <pre>
   * curl POST -H "Content-Type:text/plain" --data-binary @scientific_names.txt http://apidev.gbif.org/parser/name
   * </pre>
   */
  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  public List<ParsedName> parsePlainText(String names) {
    return parse(NEW_LINE_SPLITTER.split(Strings.nullToEmpty(names)).iterator());
  }

  private List<ParsedName> parse(Iterator<String> iter) {
    int counter = 0;
    int unparsable = 0;
    List<ParsedName> pnames = Lists.newArrayList();

    while (iter.hasNext()) {
      final String name = iter.next();
      ParsedName pn = parser.parseQuietly(name);
      pnames.add(pn);
      counter++;
      if (!pn.getType().isParsable()) {
        unparsable++;
      }
    }

    LOG.debug("Parsed {} names out of which {} are unparsable", counter, unparsable);
    return pnames;
  }
}
