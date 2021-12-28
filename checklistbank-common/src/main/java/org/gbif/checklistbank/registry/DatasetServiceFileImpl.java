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
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Grid;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * A simple implementation of a read-only DatasetService of the registry backed by a CSV file.
 */
public class DatasetServiceFileImpl extends EmptyNetworkEntityService<Dataset> implements DatasetService {
  private static final Logger LOG = LoggerFactory.getLogger(DatasetServiceFileImpl.class);

  private final TreeMap<UUID, Dataset> datasets;

  /**
   * TAB delimited file with columns:
   * key (UUID)
   * title (String)
   * dwca url (URL)
   */
  public DatasetServiceFileImpl(File dataFile) {
    datasets = Maps.newTreeMap();

    try (InputStream in = new FileInputStream(dataFile)) {
      CSVReader reader = CSVReaderFactory.buildUtf8TabReader(in);
      int endKey = 1;
      while (reader.hasNext()) {
        String[] row = reader.next();
        if (row != null && row.length >= 3 && !row[0].startsWith("#")) {
          Dataset d = new Dataset();
          d.setType(DatasetType.CHECKLIST);
          d.setKey(UUID.fromString(row[0].trim()));
          d.setTitle(row[1].trim());

          Endpoint end = new Endpoint();
          end.setKey(endKey++);
          end.setType(EndpointType.DWC_ARCHIVE);
          end.setUrl(URI.create(row[2].trim()));
          d.getEndpoints().add(end);

          datasets.put(d.getKey(), d);
        }
      }
    } catch (IOException e) {
      Throwables.propagate(e);
    }
    LOG.info("Loaded {} datasets into registry from {}", datasets.size(), dataFile.getAbsolutePath());
  }

  @Override
  public Dataset get(@NotNull UUID key) {
    return datasets.get(key);
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> keys) {
    return datasets.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey,
            e -> e.getValue().getTitle()));
  }

  @Override
  public PagingResponse<Dataset> list(@Nullable Pageable page) {
    if (page == null) {
      page = new PagingResponse<Dataset>();
    }
    PagingResponse<Dataset> resp = new PagingResponse<Dataset>();
    int idx = 1;
    for (Map.Entry<UUID, Dataset> e: datasets.entrySet()) {
      if (idx >= page.getOffset()) {
        if (idx >= page.getLimit()) {
          break;
        }
        resp.getResults().add(e.getValue());
      }
      idx++;
    }
    resp.setCount((long) datasets.size());
    return resp;
  }


  @Override
  public PagingResponse<Dataset> listConstituents(UUID datasetKey, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> listConstituents(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> listByCountry(Country country, @Nullable DatasetType type, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> listByType(DatasetType type, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Metadata> listMetadata(UUID datasetKey, @Nullable MetadataType type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Network> listNetworks(UUID datasetKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Metadata getMetadata(int metadataKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteMetadata(int metadataKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Metadata insertMetadata(UUID datasetKey, InputStream document) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getMetadataDocument(UUID datasetKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getMetadataDocument(int metadataKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> listDeleted(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> listDuplicates(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> listDatasetsWithNoEndpoint(@Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<Dataset> listByDOI(String doi, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Grid> listGrids(UUID uuid) {
    throw new UnsupportedOperationException();
  }
}
