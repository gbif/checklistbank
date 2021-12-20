package org.gbif.checklistbank.registry;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.utils.file.FileUtils;

import java.util.UUID;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertNull;

public class FileRegistryModuleTest {

  @Test
  public void configure() throws Exception {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBean(FileRegistryModule.class, FileUtils.getClasspathFile("registry-datasets.txt"));
    ctx.refresh();

    DatasetService ds = ctx.getBean(DatasetService.class);
    assertNull(ds.get(UUID.randomUUID()));

    OrganizationService os = ctx.getBean(OrganizationService.class);
    assertNull(os.get(UUID.randomUUID()));
  }

}