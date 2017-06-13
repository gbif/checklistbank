package org.gbif.checklistbank.registry;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * A simple implementation of a read-only DatasetService of the registry backed by a CSV file.
 */
public class OrganizationServiceEmptyImpl extends EmptyNetworkEntityService<Organization> implements OrganizationService {

  @Override
  public PagingResponse<Dataset> hostedDatasets(@NotNull UUID organizationKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> publishedDatasets(@NotNull UUID organizationKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Installation> installations(@NotNull UUID organizationKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Organization> listByCountry(Country country, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Organization> listDeleted(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Organization> listPendingEndorsement(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Organization> listNonPublishing(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String q) {
    throw new UnsupportedOperationException();
  }
}
