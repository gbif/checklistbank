package org.gbif.checklistbank.registry;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;

import java.util.UUID;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 *
 */
public class FileRegistryModuleTest {

  @Test
  public void configure() throws Exception {
    FileRegistryModule mod = new FileRegistryModule(Files.createTempDir());
    Injector inj = Guice.createInjector(mod);

    DatasetService ds = inj.getInstance(DatasetService.class);
    assertNull(ds.get(UUID.randomUUID()));

    OrganizationService os = inj.getInstance(OrganizationService.class);
    assertNull(os.get(UUID.randomUUID()));
  }

}