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
package org.gbif.checklistbank.cli.crawler;

import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.GenericValidationReport;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.checklistbank.cli.common.RabbitBaseService;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.common.messaging.api.messages.Platform;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.UnsupportedArchiveException;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.utils.HttpUtil;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that watches registry changed messages and does deletions of checklists and
 * updates to the dataset title table in CLB.
 */
public class CrawlerService extends RabbitBaseService<StartCrawlMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(CrawlerService.class);

  private final CrawlerConfiguration cfg;
  private final DatasetService datasetService;
  private final HttpUtil http;

  public CrawlerService(CrawlerConfiguration cfg) {
    super("clb-crawler", cfg.poolSize, cfg.messaging, cfg.ganglia);
    this.cfg = cfg;

    http = new HttpUtil(HttpUtil.newMultithreadedClient(cfg.httpTimeout, cfg.poolSize, cfg.poolSize));

    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withUrl(cfg.registry.wsUrl);
    clientBuilder.withObjectMapper(
        JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    if (cfg.registry.password != null && cfg.registry.user != null) {
      clientBuilder.withCredentials(cfg.registry.user, cfg.registry.password);
    }
    datasetService = clientBuilder.build(DatasetClient.class);
  }

  @Override
  public void handleMessage(StartCrawlMessage msg) {
    Dataset d = datasetService.get(msg.getDatasetUuid());
    if (d == null) {
      LOG.warn("No dataset known by key {}", msg.getDatasetUuid());
      return;
    }

    Optional<Endpoint> dwcaEndpoint = d.getEndpoints()
        .stream()
        .filter(e -> EndpointType.DWC_ARCHIVE.equals(e.getType()))
        .findFirst();
    if (!dwcaEndpoint.isPresent()) {
      LOG.warn("No dwc archive endpoint known for dataset {}: {}", d.getTitle(), d);
      return;
    }

    URI dwcaUri = dwcaEndpoint.get().getUrl();
    try {
      downloadAndExtract(d, dwcaUri);
      send(new DwcaMetasyncFinishedMessage(
          d.getKey(),
          d.getType(),
          dwcaUri,
          1,
          new HashMap<>(),
          new DwcaValidationReport(
              d.getKey(),
              new GenericValidationReport(1, true, new ArrayList<>(), new ArrayList<>())),
          Platform.ALL)
      );

    } catch (Exception e) {
      LOG.error("Failed to download and extract dwc archive for dataset {} from {}", d.getTitle(), dwcaUri, e);
    }
  }

  private void downloadAndExtract(Dataset d, URI dwcaUri) throws IOException, UnsupportedArchiveException {
    final File dwca = cfg.archiveFile(d.getKey());
    if (dwca.exists()) {
      dwca.delete();
      LOG.debug("Removed previous dwc archive at {}", dwca.getAbsolutePath());
    }
    http.download(dwcaUri, dwca);

    // success!
    LOG.info("Downloaded dwc archive for dataset {} from {} to {}", d.getTitle(), dwcaUri, dwca.getAbsolutePath());

    // open archive
    final File archiveDir = cfg.archiveDir(d.getKey());
    if (archiveDir.exists()) {
      FileUtils.deleteDirectory(archiveDir);
      LOG.debug("Removed previous dwc archive dir {}", dwca.getAbsolutePath());
    }
    DwcFiles.fromCompressed(dwca.toPath(), archiveDir.toPath());
    LOG.debug("Opened dwc archive successfully for dataset {} at {}", d.getTitle(), archiveDir.getAbsolutePath());
  }

  @Override
  public Class<StartCrawlMessage> getMessageClass() {
    return StartCrawlMessage.class;
  }
}
