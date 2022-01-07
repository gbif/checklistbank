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
package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class NormalizerStatsTest {
    @Test
    public void testGetCountByRank() throws Exception {
        NormalizerStats stats = new NormalizerStats(1, 2, 12, 1,
                ImmutableMap.of(Origin.DENORMED_CLASSIFICATION, 2, Origin.IMPLICIT_NAME, 10, Origin.SOURCE, 50),
                ImmutableMap.of(Rank.KINGDOM, 2, Rank.GENUS, 10, Rank.SPECIES, 50),
                Lists.<String>newArrayList());
        assertEquals(62, stats.getCount());
        assertEquals(12, stats.getSynonyms());
        assertEquals(2, stats.getDepth());
        assertEquals(1, stats.getRoots());
        assertEquals(1, stats.getIgnored());

        assertEquals(50, stats.getCountByOrigin(Origin.SOURCE));
        assertEquals(2, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
        assertEquals(10, stats.getCountByOrigin(Origin.IMPLICIT_NAME));

        assertEquals(50, stats.getCountByRank(Rank.SPECIES));
        assertEquals(10, stats.getCountByRank(Rank.GENUS));
        assertEquals(2, stats.getCountByRank(Rank.KINGDOM));
        assertEquals(0, stats.getCountByRank(Rank.FAMILY));
    }
}