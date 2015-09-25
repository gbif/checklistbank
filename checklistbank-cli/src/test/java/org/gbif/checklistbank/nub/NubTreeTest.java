package org.gbif.checklistbank.nub;

import org.gbif.utils.file.FileUtils;

import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class NubTreeTest {

    @Test
    public void testRead() throws Exception {
        NubTree tree = NubTree.read(FileUtils.classpathStream("trees/test.txt"));

        tree.print(System.out);

        StringWriter buffer = new StringWriter();
        tree.print(buffer);
        assertEquals(buffer.toString().trim(), IOUtils.toString(FileUtils.classpathStream("trees/test.txt"), "UTF8").trim());
    }

}