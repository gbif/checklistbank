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
package org.gbif.checklistbank.ws.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;

public class LineReader implements Iterable<String> {

  private BufferedReader r;

  public LineReader(InputStream stream, Charset encoding) {
    this.r = new BufferedReader(new InputStreamReader(stream, encoding));
  }

  public LineReader(BufferedReader r) {
    this.r = r;
  }

  @Override
  public Iterator<String> iterator() {
    return new Iterator<String>() {

      @Override
      public boolean hasNext() {
        try {
          r.mark(1);
          if (r.read() < 0) {
            return false;
          }
          r.reset();
          return true;
        } catch (IOException e) {
          return false;
        }
      }

      @Override
      public String next() {
        try {
          return r.readLine();
        } catch (IOException e) {
          return null;
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

}