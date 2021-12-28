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

import java.io.Closeable;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CloseableUtils {
  private static final Logger LOG = LoggerFactory.getLogger(CloseableUtils.class);


  public static void close(Collection<Closeable> toBeClosed) {
    for (Closeable cl : toBeClosed) {
      try {
        cl.close();
      } catch (Exception e) {
        LOG.error("Failed to close {}", cl.getClass().getSimpleName(), e);
      }
    }
  }

  public static void close(Iterable<AutoCloseable> toBeClosed) {
    for (AutoCloseable cl : toBeClosed) {
      try {
        cl.close();
      } catch (Exception e) {
        LOG.error("Failed to close {}", cl.getClass().getSimpleName(), e);
      }
    }
  }
}
