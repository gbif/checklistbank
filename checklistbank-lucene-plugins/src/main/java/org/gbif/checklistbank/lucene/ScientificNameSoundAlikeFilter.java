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
package org.gbif.checklistbank.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Based on character transpositions found in Tony Reese's TaxonMatch.
 */
public class ScientificNameSoundAlikeFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Construct a token stream filtering the given input.
   */
  public ScientificNameSoundAlikeFilter(TokenStream input) {
    super(input);
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (!input.incrementToken()) return false;

    final char[] buffer = termAtt.buffer();
    final int bufferLength = termAtt.length();

    // Do some selective replacement on the leading letter/s only:
    if (bufferLength > 2) {
      String start3 = new String(buffer, 0, 2);
      if (start3.startsWith("ae")) {
        start3 = "E" + start3.substring(2);
      } else if (start3.startsWith("cn")) {
        start3 = "N" + start3.substring(2);
      } else if (start3.startsWith("ct")) {
        start3 = "T" + start3.substring(2);
      } else if (start3.startsWith("cz")) {
        start3 = "C" + start3.substring(2);
      } else if (start3.startsWith("dj")) {
        start3 = "J" + start3.substring(2);
      } else if (start3.startsWith("ea")) {
        start3 = "E" + start3.substring(2);
      } else if (start3.startsWith("eu")) {
        start3 = "U" + start3.substring(2);
      } else if (start3.startsWith("gn")) {
        start3 = "N" + start3.substring(2);
      } else if (start3.startsWith("kn")) {
        start3 = "N" + start3.substring(2);
      } else if (start3.startsWith("mc")) {
        start3 = "MAC" + start3.substring(2);
      } else if (start3.startsWith("mn")) {
        start3 = "N" + start3.substring(2);
      } else if (start3.startsWith("oe")) {
        start3 = "E" + start3.substring(2);
      } else if (start3.startsWith("qu")) {
        start3 = "Q" + start3.substring(2);
      } else if (start3.startsWith("ps")) {
        start3 = "S" + start3.substring(2);
      } else if (start3.startsWith("pt")) {
        start3 = "T" + start3.substring(2);
      } else if (start3.startsWith("ts")) {
        start3 = "S" + start3.substring(2);
      } else if (start3.startsWith("wr")) {
        start3 = "R" + start3.substring(2);
      } else if (start3.startsWith("x")) {
        start3 = "Z" + start3.substring(2);
      }
    }

    // Now keep the leading character, then do selected "soundalike" replacements.
    // The following letters are equated: AE, OE, E, U, Y and I; IA and A are equated;
    // K and C; Z and S; and H is dropped.
    // Also, A and O are equated, MAC and MC are equated, and SC and S.

    if (bufferLength > 1) {
      int upto = Character.isWhitespace(buffer[0]) ? 0 : 1;
      char c1 = ' ';
      for(int i=1; i<bufferLength; i++) {
        char c = buffer[i];
        boolean skip = false;

        // replace all whitespace with spaces
        if (Character.isWhitespace(c)) {
          c = ' ';

        } else {

          switch (buffer[i]) {
            case 'a':
              if (c1 == 'i') {
                upto--;
                c='a';
              }
              break;
            case 'c':
              if (c1 == 's') {
                skip = true;
              }
              break;
            case 'e':
              if (c1 == 'a' || c1 == 'o') {
                upto--;
              }
              c='i';
              break;
            case 'i':
              if (c1 == 'o') {
                upto--;
                c='a';
              }
              break;
            case 'o': c='a'; break;
            case 'u': c='i'; break;
            case 'y': c='i'; break;
            case 'k': c='c'; break;
            case 'z': c='c'; break;
            case 'h': skip=true; break;
          }
        }

        // drop any repeated characters (AA becomes A, BB or BBB becomes B, etc.)
        if (!skip && upto > 0 && buffer[upto-1] == c) {
          skip = true;
        }

        // remember original char
        c1 = buffer[i];

        // alter buffer
        if (!skip) {
          buffer[upto++] = c;
        }
      }
      termAtt.setLength(upto);
    }

    //termAtt.setEmpty().append(treatWord(termAtt.toString(), false));

    return true;
  }

  public static String treatWord(String str2, boolean isSpecies) {
    char startLetter;

    String temp = str2.toUpperCase();
    // Do some selective replacement on the leading letter/s only:

    if (temp.startsWith("AE")) {
      temp = "E" + temp.substring(2);
    } else if (temp.startsWith("CN")) {
      temp = "N" + temp.substring(2);
    } else if (temp.startsWith("CT")) {
      temp = "T" + temp.substring(2);
    } else if (temp.startsWith("CZ")) {
      temp = "C" + temp.substring(2);
    } else if (temp.startsWith("DJ")) {
      temp = "J" + temp.substring(2);
    } else if (temp.startsWith("EA")) {
      temp = "E" + temp.substring(2);
    } else if (temp.startsWith("EU")) {
      temp = "U" + temp.substring(2);
    } else if (temp.startsWith("GN")) {
      temp = "N" + temp.substring(2);
    } else if (temp.startsWith("KN")) {
      temp = "N" + temp.substring(2);
    } else if (temp.startsWith("MC")) {
      temp = "MAC" + temp.substring(2);
    } else if (temp.startsWith("MN")) {
      temp = "N" + temp.substring(2);
    } else if (temp.startsWith("OE")) {
      temp = "E" + temp.substring(2);
    } else if (temp.startsWith("QU")) {
      temp = "Q" + temp.substring(2);
    } else if (temp.startsWith("PS")) {
      temp = "S" + temp.substring(2);
    } else if (temp.startsWith("PT")) {
      temp = "T" + temp.substring(2);
    } else if (temp.startsWith("TS")) {
      temp = "S" + temp.substring(2);
    } else if (temp.startsWith("WR")) {
      temp = "R" + temp.substring(2);
    } else if (temp.startsWith("X")) {
      temp = "Z" + temp.substring(2);
    }
    // Now keep the leading character, then do selected "soundalike" replacements. The
    // following letters are equated: AE, OE, E, U, Y and I; IA and A are equated;
    // K and C; Z and S; and H is dropped. Also, A and O are equated, MAC and MC are equated, and SC and S.
    startLetter = temp.charAt(0); // quarantine the leading letter
    temp = temp.substring(1); // snip off the leading letter
    // now do the replacements
    temp = temp.replaceAll("AE", "I");
    temp = temp.replaceAll("IA", "A");
    temp = temp.replaceAll("OE", "I");
    temp = temp.replaceAll("OI", "A");
    temp = temp.replaceAll("SC", "S");
    temp = temp.replaceAll("E", "I");
    temp = temp.replaceAll("O", "A");
    temp = temp.replaceAll("U", "I");
    temp = temp.replaceAll("Y", "I");
    temp = temp.replaceAll("K", "C");
    temp = temp.replaceAll("Z", "C");
    temp = temp.replaceAll("H", "");
    // add back the leading letter
    temp = startLetter + temp;
    // now drop any repeated characters (AA becomes A, BB or BBB becomes B, etc.)
    temp = temp.replaceAll("(\\w)\\1+", "$1");

    if (isSpecies) {
      if (temp.endsWith("IS")) {
        temp = temp.substring(0, temp.length() - 2) + "A";
      } else if (temp.endsWith("IM")) {
        temp = temp.substring(0, temp.length() - 2) + "A";
      } else if (temp.endsWith("AS")) {
        temp = temp.substring(0, temp.length() - 2) + "A";
      }
      //temp = temp.replaceAll("(\\w)\\1+", "$1");
    }

    return temp;
  }
}
