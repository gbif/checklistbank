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
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.NodeRequestSearchParams;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * A simple implementation of a read-only DatasetService of the registry backed by a CSV file.
 */
public class NodeServiceEmptyImpl extends EmptyNetworkEntityService<Node> implements NodeService {

  @Override
  public PagingResponse<Organization> endorsedOrganizations(@NotNull UUID nodeKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Organization> pendingEndorsements(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Organization> pendingEndorsements(@NotNull UUID nodeKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Installation> installations(@NotNull UUID nodeKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Node getByCountry(Country country) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Country> listNodeCountries() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Country> listActiveCountries() {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> endorsedDatasets(@NotNull UUID nodeKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Node> list(NodeRequestSearchParams nodeRequestSearchParams) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Node> listByMachineTag(String namespace, @Nullable String name, @Nullable String value, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addMachineTag(UUID targetEntityKey, TagName tagName, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagNamespace tagNamespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagName tagName) {
    throw new UnsupportedOperationException();
  }
}
