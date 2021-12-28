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
package org.gbif.checklistbank.utils;

import org.gbif.api.vocabulary.Kingdom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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