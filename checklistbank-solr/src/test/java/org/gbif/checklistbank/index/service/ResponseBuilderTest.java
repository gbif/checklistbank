package org.gbif.checklistbank.index.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ResponseBuilderTest {

  @Test
  public void mergeHl() throws Exception {
    assertEquals("Jeden Abend gehe ich von <em class=\"gbifHl\">der Arbeit</em> in den Zoologischen Garten.", ResponseBuilder.mergeHl("Jeden Abend gehe ich von der Arbeit in den Zoologischen Garten.", "<em class=\"gbifHl\">der Arbeit</em>"));
  }

}