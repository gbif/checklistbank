package org.gbif.checklistbank.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CleanupUtilsTest {

  @Test
  public void testClean() throws Exception {
    assertNull(CleanupUtils.clean(null));
    assertNull(CleanupUtils.clean(" "));
    assertNull(CleanupUtils.clean("   "));
    assertNull(CleanupUtils.clean("\\N"));
    assertNull(CleanupUtils.clean("NULL"));
    assertNull(CleanupUtils.clean("\t "));
    assertNull(CleanupUtils.clean("\u0000"));
    assertNull(CleanupUtils.clean("\u0001"));
    assertNull(CleanupUtils.clean("\u0002"));

    assertEquals("Abies", CleanupUtils.clean("Abies"));
    assertEquals("öAbies", CleanupUtils.clean("öAbies"));
    assertEquals("Abies mille", CleanupUtils.clean(" Abies  mille"));
    assertEquals("Abies x", CleanupUtils.clean("Abies\u0000x"));
    assertEquals("Abies x", CleanupUtils.clean("Abies\u0000\u0000\u0000x"));
    assertEquals("Abies x", CleanupUtils.clean("Abies\u0000\u0001x"));

    assertNull(CleanupUtils.clean(""));
    assertNull(CleanupUtils.clean("null"));
    assertNull(CleanupUtils.clean("null  "));
    assertEquals("hi Pete", CleanupUtils.clean("hi  Pete "));
    assertEquals("hi Pete", CleanupUtils.clean("hi  Pete "));
    assertEquals("hi Pete", CleanupUtils.clean("hi  Pete "));
    assertEquals("öüä", CleanupUtils.clean("öüä")); // 2 byte encodings
    // 3 byte encodings using the combining diaresis - visually entirely different, but not in raw bytes!
    assertEquals("Bärmann, Fürst von Lieven & Sudhaus, 2009", CleanupUtils.clean("Bärmann, Fürst von Lieven & Sudhaus, 2009"));
    assertEquals("Niä", CleanupUtils.clean("Nia"+'\u0308')); // combining diaresis
    assertEquals("Nin̆a", CleanupUtils.clean("Nin"+'\u0306' +"a")); // combining breve
    assertEquals("Niña", CleanupUtils.clean("Nin"+'\u0303' +"a")); // combining tilde
    assertEquals("Niéa", CleanupUtils.clean("Nie"+'\u0301' +"a")); // combining Acute Accent
  }

}