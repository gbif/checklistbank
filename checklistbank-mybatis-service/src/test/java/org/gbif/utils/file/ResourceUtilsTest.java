package org.gbif.utils.file;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test the resources method again from outside the original module, so we can test how included jars behave!
 */
public class ResourceUtilsTest {

    @Test
    public void testList() throws Exception {
        assertArrayEquals(new String[]{}, ResourcesUtil.list(ResourcesUtil.class, "abba"));
        assertArrayEquals(new String[]{"authormap.txt"}, ResourcesUtil.list(ResourcesUtil.class, "authorship"));
        assertArrayEquals(new String[]{"citation.tsv", "dataset.tsv", "dataset_metrics.tsv", "description.tsv", "distribution.tsv", "identifier.tsv", "literature.tsv",
                "media.tsv", "name.tsv", "name_usage.tsv", "name_usage_metrics.tsv", "nub_rel.tsv", "species_info.tsv", "typification.tsv", "vernacular_name.tsv"},
                ResourcesUtil.list(ResourcesUtil.class, "squirrels"));
    }
}