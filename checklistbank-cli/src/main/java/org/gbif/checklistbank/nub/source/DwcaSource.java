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
package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.dwc.DwcFiles;
import org.gbif.utils.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

/**
 * A nub source which is backed by a dwca checklist file which gets normalized into neo4j first
 * and then drives the nub source
 */
public class DwcaSource extends NubSource {
  private static final Logger LOG = LoggerFactory.getLogger(DwcaSource.class);
  private static HttpUtil http = new HttpUtil(HttpUtil.newMultithreadedClient(10000,10,2));
  // we use a different key for the dwca normalizer so we dont clash with the real nubsource key
  private final UUID normalizerKey = UUID.randomUUID();

  private NormalizerConfiguration cfg = new NormalizerConfiguration();

  public DwcaSource(String name, File dwca, NeoConfiguration neo) throws IOException {
    super(UUID.randomUUID(), name.replaceAll("\\s", " "), null, false, neo);
    initRepos();
    File archiveDir = cfg.archiveDir(normalizerKey);
    LOG.info("Open dwc archive {}", dwca);
    DwcFiles.fromCompressed(dwca.toPath(), archiveDir.toPath());
  }

  public DwcaSource(String name, URL dwca, NeoConfiguration neo) throws IOException {
    this(name, download(dwca), neo);
  }

  private void initRepos() {
    cfg.archiveRepository = Files.createTempDir();
    cfg.neo = super.cfg;
  }

  private static File download(URL dwca) throws IOException {
    final File tmp = File.createTempFile("dwca-download", "dwca");
    LOG.info("Download dwca from {} into {}", dwca, tmp);
    http.download(dwca, tmp);
    return tmp;
  }

  private static String nullsafeString(@Nullable Object obj) {
    return obj == null ? null : obj.toString();
  }

  @Override
  public void initNeo(NeoUsageWriter writer) throws Exception {
    UsageDao dao = normalize();
    LOG.info("Import source usages");
    try (Transaction tx = dao.beginTx()) {
      for (Node n : dao.allNodes()) {
        NameUsage u = dao.readUsage(n, true);
        TaxonomicStatus status = u.getTaxonomicStatus();
        if (status == null) {
          status = u.isSynonym() ? TaxonomicStatus.SYNONYM : TaxonomicStatus.ACCEPTED;
        }
        String[] row = new String[8];
        row[0] = String.valueOf(n.getId());
        row[1] = nullsafeString(status.isSynonym() ? u.getAcceptedKey() : u.getParentKey());
        row[2] = nullsafeString(u.getBasionymKey());
        row[3] = nullsafeString(u.getRank());
        row[4] = status.name();
        //TODO: nom status
        row[5] = null;
        row[6] = u.getScientificName();
        row[7] = u.getPublishedIn();
        writer.addRow(row);
      }
    }
    dao.closeAndDelete();
  }

  /**
   * read dwca stream and normalize it
   */
  private UsageDao normalize() {
    LOG.info("Normalize dwca, key={}", normalizerKey);
    Normalizer normalizer = Normalizer.create(cfg, normalizerKey);
    normalizer.run();
    return UsageDao.open(cfg.neo, normalizerKey);
  }

  @Override
  public void close() {
    super.close();
    FileUtils.deleteQuietly(cfg.archiveRepository);
    FileUtils.deleteQuietly(cfg.neo.neoRepository);
  }
}
