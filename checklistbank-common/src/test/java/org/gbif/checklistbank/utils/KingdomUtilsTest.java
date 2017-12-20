package org.gbif.checklistbank.utils;

import org.gbif.api.vocabulary.Kingdom;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class KingdomUtilsTest {

  @Test
  public void match() throws Exception {
    KingdomUtils.logMatrix();

    assertFalse(KingdomUtils.match(Kingdom.ANIMALIA, Kingdom.PLANTAE));
    assertFalse(KingdomUtils.match(Kingdom.ANIMALIA, Kingdom.FUNGI));
    assertFalse(KingdomUtils.match(Kingdom.ANIMALIA, Kingdom.BACTERIA));
    assertFalse(KingdomUtils.match(Kingdom.ANIMALIA, Kingdom.VIRUSES));

    assertFalse(KingdomUtils.match(Kingdom.PLANTAE, Kingdom.ANIMALIA));
    assertFalse(KingdomUtils.match(Kingdom.PLANTAE, Kingdom.FUNGI));
    assertFalse(KingdomUtils.match(Kingdom.PLANTAE, Kingdom.BACTERIA));
    assertFalse(KingdomUtils.match(Kingdom.PLANTAE, Kingdom.VIRUSES));

    assertTrue(KingdomUtils.match(Kingdom.ANIMALIA, Kingdom.PROTOZOA));
    assertTrue(KingdomUtils.match(Kingdom.ANIMALIA, Kingdom.CHROMISTA));

    assertTrue(KingdomUtils.match(Kingdom.PLANTAE, Kingdom.PROTOZOA));
    assertTrue(KingdomUtils.match(Kingdom.PLANTAE, Kingdom.CHROMISTA));

    assertTrue(KingdomUtils.match(Kingdom.FUNGI, Kingdom.PROTOZOA));
    assertTrue(KingdomUtils.match(Kingdom.FUNGI, Kingdom.CHROMISTA));
  }

}