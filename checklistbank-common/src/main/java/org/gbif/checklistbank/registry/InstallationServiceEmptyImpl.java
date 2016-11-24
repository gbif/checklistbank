package org.gbif.checklistbank.registry;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.InstallationService;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * A simple implementation of a read-only DatasetService of the registry backed by a CSV file.
 */
public class InstallationServiceEmptyImpl extends EmptyNetworkEntityService<Installation> implements InstallationService {

  @Override
  public PagingResponse<Dataset> getHostedDatasets(@NotNull UUID installationKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Installation> listDeleted(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Installation> listNonPublishing(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }
}
