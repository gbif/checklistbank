package org.gbif.checklistbank.nub;

import org.gbif.utils.file.FileUtils;

import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class NubTreeTest {

  @Test
  public void testRead() throws Exception {
    NubTree tree = NubTree.read(FileUtils.classpathStream("trees/test.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(buffer.toString().trim(), IOUtils.toString(FileUtils.classpathStream("trees/test.txt"), "UTF8").trim());


    System.out.println("Tree traversal");
    for (NubNode n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      assertNull(n.rank);
    }
  }

  @Test
  public void testRead2() throws Exception {
    NubTree tree = NubTree.read(FileUtils.classpathStream("trees/test2.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(buffer.toString().trim(), IOUtils.toString(FileUtils.classpathStream("trees/test2.txt"), "UTF8").trim());


    System.out.println("Tree traversal");
    for (NubNode n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      assertNotNull(n.rank);
    }
  }

}