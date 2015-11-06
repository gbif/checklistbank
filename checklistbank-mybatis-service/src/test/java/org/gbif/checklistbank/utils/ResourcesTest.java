package org.gbif.checklistbank.utils;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/**
 * Test the resources method again from outside the original module, so we can test how included jars behave!
 */
public class ResourcesTest {

    @Test
    public void testGetResourceListing() throws Exception {
        assertArrayEquals(new String[]{"authormap.txt"}, Resources.getResourceListing(Resources.class, "authorship"));
        assertArrayEquals(new String[]{"citation.tsv", "dataset.tsv", "dataset_metrics.tsv", "description.tsv", "distribution.tsv", "identifier.tsv", "literature.tsv",
                "media.tsv", "name.tsv", "name_usage.tsv", "name_usage_metrics.tsv", "nub_rel.tsv", "species_info.tsv", "typification.tsv", "vernacular_name.tsv"},
                Resources.getResourceListing(Resources.class, "squirrels"));
    }

    @Test(expected = IOException.class)
    public void testGetResourceListingNone() throws Exception {
        assertNull(Resources.getResourceListing(Resources.class, "abba"));
    }
}