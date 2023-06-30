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
import org.gbif.api.model.registry.search.InstallationRequestSearchParams;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.vocabulary.InstallationType;

import java.util.List;
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
  public PagingResponse<Installation> listDeleted(InstallationRequestSearchParams installationRequestSearchParams) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Installation> listNonPublishing(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Installation> listByType(
    InstallationType installationType, @Nullable Pageable pageable
  ) {
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public PagingResponse<Installation> list(InstallationRequestSearchParams installationRequestSearchParams) {
    return null;
  }
}
