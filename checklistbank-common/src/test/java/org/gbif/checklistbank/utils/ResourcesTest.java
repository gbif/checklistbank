package org.gbif.checklistbank.utils;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class ResourcesTest {

    @Test
    public void testGetResourceListing() throws Exception {
        assertArrayEquals(new String[]{"authormap.txt"}, Resources.getResourceListing(Resources.class, "authorship"));
    }

    @Test(expected = IOException.class)
    public void testGetResourceListingNone() throws Exception {
        assertNull(Resources.getResourceListing(Resources.class, "abba"));
    }
}