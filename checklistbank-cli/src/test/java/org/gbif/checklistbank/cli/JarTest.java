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
package org.gbif.checklistbank.cli;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Manual terst to inspect the uber jar to list number of files to get an idea whats inside.
 */
@Ignore
public class JarTest {

    @Test
    public void countJar() throws IOException {
        JarFile jar = new JarFile("target/checklistbank-cli.jar");

        System.out.println(
                Collections.list(jar.entries()).size()
        );
        Pattern rootPath = Pattern.compile("^(org\\/(apache|neo4j(\\/cypher\\/internal\\/compiler)?)|[a-z0-9_-]+)\\/[a-z0-9_-]+", Pattern.CASE_INSENSITIVE);
        Map<String, AtomicInteger> counts = new TreeMap<String, AtomicInteger>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            String key;
            Matcher m = rootPath.matcher(e.getName());
            if (m.find()) {
                key = m.group(0);
            } else {
                key = e.getName();
            }
            if (!counts.containsKey(key)) {
                counts.put(key, new AtomicInteger(1));
            } else {
                counts.get(key).getAndIncrement();
            }
        }

        for (Map.Entry<String, AtomicInteger> e : counts.entrySet()) {
            if (e.getValue().get() > 100) {
                System.out.println(e.getKey() + "   " + e.getValue());
            }
        }
    }
}
