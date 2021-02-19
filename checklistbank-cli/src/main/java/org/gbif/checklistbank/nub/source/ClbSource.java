package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

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
