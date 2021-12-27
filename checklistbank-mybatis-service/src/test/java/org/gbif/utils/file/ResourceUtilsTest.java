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
package org.gbif.utils.file;

import org.junit.Test;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Test the resources method again from outside the original module, so we can test how included jars behave!
 */
public class ResourceUtilsTest {

    @Test
    public void testList() throws Exception {
        assertArrayEquals(new String[]{}, ResourcesUtil.list(ResourcesUtil.class, "abba"));
        assertEquals(Sets.newHashSet("authormap.txt"), Sets.newHashSet(ResourcesUtil.list(ResourcesUtil.class, "authorship")));
        assertEquals(Sets.newHashSet("citation.tsv", "dataset.tsv", "dataset_metrics.tsv", "description.tsv", "distribution.tsv", "identifier.tsv", "literature.tsv",
                "media.tsv", "name.tsv", "name_usage.tsv", "name_usage_metrics.tsv", "nub_rel.tsv", "species_info.tsv", "typification.tsv", "vernacular_name.tsv"),
                Sets.newHashSet(ResourcesUtil.list(ResourcesUtil.class, "squirrels")));
    }
}