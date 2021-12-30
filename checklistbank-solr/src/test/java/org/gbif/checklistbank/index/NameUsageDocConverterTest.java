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
package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.List;
import java.util.UUID;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameUsageDocConverterTest {

    @Test
    public void testToObject() throws Exception {
        NameUsageDocConverter conv = new NameUsageDocConverter();

        NameUsage u = new NameUsage();
        u.setKey(12);
        u.setDatasetKey(UUID.randomUUID());
        u.setScientificName("Abies alba Mill.");
        u.setCanonicalName("Abies alba");
        u.setRank(Rank.SPECIES);
        u.setOrigin(Origin.SOURCE);
        u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
        u.setParentKey(1);
        u.setFamilyKey(1024);
        u.setFamily("Pinaceae");
        u.getIssues().add(NameUsageIssue.RANK_INVALID);
        u.getIssues().add(NameUsageIssue.CLASSIFICATION_NOT_APPLIED);

        UsageExtensions ext = new UsageExtensions();
        SpeciesProfile sp = new SpeciesProfile();
        sp.setTerrestrial(true);
        sp.setHabitat("brackish");
        ext.speciesProfiles.add(sp);

        VernacularName v1 = new VernacularName();
        v1.setLanguage(Language.GERMAN);
        v1.setVernacularName("Weißtanne");
        VernacularName v2 = new VernacularName();
        v2.setLanguage(Language.GERMAN);
        v2.setVernacularName("Kohl Tanne");
        ext.vernacularNames.add(v1);
        ext.vernacularNames.add(v2);

        SolrInputDocument doc = conv.toDoc(u, Lists.newArrayList(12, 15, 20, 100), ext);

        assertEquals(u.getKey(), doc.get("key").getValue());
        assertEquals(u.getDatasetKey().toString(), doc.get("dataset_key").getValue());
        assertEquals(u.getParentKey(), doc.get("parent_key").getValue());
        assertEquals(u.getFamily(), doc.get("family").getValue());
        assertEquals(u.getFamilyKey(), doc.get("family_key").getValue());
        assertEquals(u.getScientificName(), doc.get("scientific_name").getValue());
        assertEquals(u.getCanonicalName(), doc.get("canonical_name").getValue());
        assertEquals(u.getOrigin().ordinal(), doc.get("origin_key").getValue());
        assertEquals(u.getTaxonomicStatus().ordinal(), doc.get("taxonomic_status_key").getValue());
        assertEquals(u.getRank().ordinal(), doc.get("rank_key").getValue());
        assertThat((List<Integer>) doc.get("issues").getValue()).contains(NameUsageIssue.RANK_INVALID.ordinal(),
                NameUsageIssue.CLASSIFICATION_NOT_APPLIED.ordinal());
        assertThat((List<Integer>) doc.get("habitat_key").getValue()).containsOnlyOnce(Habitat.TERRESTRIAL.ordinal(), Habitat.FRESHWATER.ordinal());
        assertThat((List<Integer>) doc.get("higher_taxon_key").getValue()).containsOnlyOnce(12, 15, 20, 100);

        // trip back
        NameUsageSearchResult u2 = conv.toSearchUsage(ClientUtils.toSolrDocument(doc), true);
        assertEquals(ClassificationUtils.getHigherClassificationMap(u2), ClassificationUtils.getHigherClassificationMap(u));
        assertEquals(u2.getKey(), u.getKey());
        assertEquals(u2.getDatasetKey(), u.getDatasetKey());
        assertEquals(u2.getParentKey(), u.getParentKey());
        assertEquals(u2.getScientificName(), u.getScientificName());
        assertEquals(u2.getCanonicalName(), u.getCanonicalName());
        assertEquals(u2.getOrigin(), u.getOrigin());
        assertEquals(u2.getTaxonomicStatus(), u.getTaxonomicStatus());
        assertEquals(u2.getRank(), u.getRank());
    }
}