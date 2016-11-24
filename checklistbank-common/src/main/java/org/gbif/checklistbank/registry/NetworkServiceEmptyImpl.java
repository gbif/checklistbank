package org.gbif.checklistbank.registry;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.NetworkService;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A simple implementation of a read-only DatasetService of the registry backed by a CSV file.
 */
public class NetworkServiceEmptyImpl extends EmptyNetworkEntityService<Network> implements NetworkService {

  @Override
  public PagingResponse<Dataset> listConstituents(UUID networkKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addConstituent(UUID networkKey, UUID datasetKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeConstituent(UUID networkKey, UUID datasetKey) {
    throw new UnsupportedOperationException();
  }
}
