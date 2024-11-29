/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.registry;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.OrganizationRequestSearchParams;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.geojson.FeatureCollection;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

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
  public PagingResponse<Organization> listDeleted(OrganizationRequestSearchParams organizationRequestSearchParams) {
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

  @Override
  public boolean confirmEndorsement(UUID organizationKey, UUID confirmationKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean confirmEndorsement(@NotNull UUID uuid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean revokeEndorsement(@NotNull UUID uuid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Organization> list(OrganizationRequestSearchParams organizationRequestSearchParams) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FeatureCollection listGeoJson(OrganizationRequestSearchParams organizationRequestSearchParams) {
    throw new UnsupportedOperationException();
  }
}
