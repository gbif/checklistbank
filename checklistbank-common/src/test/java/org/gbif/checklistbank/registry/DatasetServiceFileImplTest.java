package org.gbif.checklistbank.registry;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.utils.file.FileUtils;

import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DatasetServiceFileImplTest {

  @Test
  public void get() throws Exception {

    DatasetService ds = new DatasetServiceFileImpl(FileUtils.getClasspathFile("registry-datasets.txt"));
    UUID key = UUID.fromString("c696e5ee-9088-4d11-bdae-ab88daffab78");
    assertEquals(key, ds.get(key).getKey());
    System.out.println(ds.get(key).toString());
  }

}