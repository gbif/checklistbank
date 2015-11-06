package org.gbif.utils.file;

import com.google.common.collect.Sets;
import org.junit.Test;

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