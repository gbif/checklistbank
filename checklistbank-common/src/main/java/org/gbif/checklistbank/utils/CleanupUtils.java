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

import java.io.File;
import java.text.Normalizer;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanupUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupUtils.class);
    private static final Pattern NULL_PATTERN = Pattern.compile("^\\s*(\\\\N|\\\\?NULL|null)\\s*$");
    private static final CharMatcher SPACE_MATCHER = CharMatcher.whitespace().or(CharMatcher.javaIsoControl());

    public static void registerCleanupHook(final File f) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                if (f.exists()) {
                    LOG.debug("Deleting file {}", f.getAbsolutePath());
                    FileUtils.deleteQuietly(f);
                }
            }
        });
    }

    /**
     * Does a conservative, generic cleaning of strings including:
     *  - trims and replaces various whitespace and invisible control characters
     *  - remove common verbatim values for NULL
     *  - normalises unicode into the NFC form
     */
    public static String clean(String x) {
        if (Strings.isNullOrEmpty(x) || NULL_PATTERN.matcher(x).find()) {
            return null;
        }
        x = SPACE_MATCHER.trimAndCollapseFrom(x, ' ');
        // normalise unicode into NFC
        x = Normalizer.normalize(x, Normalizer.Form.NFC);
        return Strings.emptyToNull(x.trim());
    }

}
