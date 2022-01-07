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
package org.gbif.checklistbank.nub.model;

public class NubUsageMatch {
  public final NubUsage usage;
  public final boolean ignore;
  public final NubUsage doubtfulUsage;

  private NubUsageMatch(NubUsage usage, boolean ignore, NubUsage doubtfulUsage) {
    this.usage = usage;
    this.ignore = ignore;
    this.doubtfulUsage = doubtfulUsage;
  }

  public static NubUsageMatch match(NubUsage usage) {
    return new NubUsageMatch(usage, false, null);
  }

  /**
   * Snaps to a usage but flag it to be ignored in immediate processing.
   */
  public static NubUsageMatch snap(NubUsage usage) {
    return new NubUsageMatch(usage, true, null);
  }

  public static NubUsageMatch empty() {
    return new NubUsageMatch(null, false, null);
  }

  public static NubUsageMatch empty(NubUsage doubtfulUsage) {
    return new NubUsageMatch(null, false, doubtfulUsage);
  }

  public boolean isMatch() {
    return usage != null;
  }
}
