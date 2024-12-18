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

import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.config.ClbConfiguration;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A nub source which is backed by postgres checklistbank usages of a given datasetKey
 */
public class ClbSource extends NubSource {
  private static final Logger LOG = LoggerFactory.getLogger(ClbSource.class);
  private final ClbConfiguration clb;

  public ClbSource(ClbConfiguration clb, NeoConfiguration neo, UUID key, String name) {
    this(clb, neo, key, name, null);
  }

  public ClbSource(ClbConfiguration clb, NeoConfiguration neo, UUID key, String name, @Nullable List<RankedName> exclusion) {
    super(key, name.replaceAll("\\s", " "), exclusion, false, neo);
    this.clb = clb;
  }

  public ClbSource(ClbConfiguration clb, NeoConfiguration neo, Dataset dataset, @Nullable List<RankedName> exclusion) {
    this(clb, neo, dataset.getKey(), dataset.getTitle(), exclusion);
  }

  public ClbSource(ClbConfiguration clb, NeoConfiguration neo, Dataset dataset) {
    this(clb, neo, dataset, null);
  }

  @Override
  public void initNeo(NeoUsageWriter writer) throws Exception {
    try (BaseConnection c = (BaseConnection) clb.connect()) {
      final CopyManager cm = new CopyManager(c);
      cm.copyOut("COPY ("
          + "SELECT u.id, u.parent_fk, u.basionym_fk, u.rank,"
          + " coalesce(u.status, CASE WHEN (u.is_synonym) THEN 'SYNONYM'::taxonomic_status ELSE 'ACCEPTED'::taxonomic_status END),"
          + " u.nom_status, n.scientific_name, c.citation"
          + " FROM name_usage u JOIN name n ON u.name_fk=n.id LEFT JOIN citation c ON u.name_published_in_fk=c.id"
          + " WHERE u.dataset_key = '" + key + "')"
          + " TO STDOUT WITH NULL ''", writer);
    }
  }
}
