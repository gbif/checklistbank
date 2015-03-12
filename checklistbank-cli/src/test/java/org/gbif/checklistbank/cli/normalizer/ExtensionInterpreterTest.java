package org.gbif.checklistbank.cli.normalizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExtensionInterpreterTest {

  @Test
  public void testExpandGenus() throws Exception {
    assertEquals(null, ExtensionInterpreter.expandGenus(null, "Abies alba"));
    assertEquals("", ExtensionInterpreter.expandGenus("", "Abies"));

    assertEquals("Abies alba", ExtensionInterpreter.expandGenus("Abies alba", "Abies negra"));
    assertEquals("Abies alba", ExtensionInterpreter.expandGenus("Abies alba", null));
    assertEquals("Abies alba", ExtensionInterpreter.expandGenus("A alba", "Abies negra"));
    assertEquals("Abies alba", ExtensionInterpreter.expandGenus("A. alba", "Abies negra"));
    assertEquals("Abies alba", ExtensionInterpreter.expandGenus("A.  alba", "Abies negra"));
  }
}