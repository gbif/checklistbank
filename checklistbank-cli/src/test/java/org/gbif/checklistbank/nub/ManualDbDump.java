package org.gbif.checklistbank.nub;

import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.show.GraphFormat;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.source.ClbSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualDbDump {
  private static final Logger LOG = LoggerFactory.getLogger(ManualDbDump.class);

  private static NeoConfiguration neo() {
    NeoConfiguration cfg = new NeoConfiguration();
    cfg.neoRepository = new File("/Users/markus/Desktop/repo/neo");
    cfg.cacheType = NeoConfiguration.CacheType.SOFT;
    cfg.mappedMemory = 1024;
    return cfg;
  }

  private static ClbConfiguration local() {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.serverName = "localhost";
    cfg.databaseName = "clb";
    cfg.user = "postgres";
    cfg.password = "pogo";
    return cfg;
  }

  private static ClbConfiguration uat() {
    ClbConfiguration cfg = local();
    cfg.serverName = "pg1.gbif-uat.org";
    cfg.databaseName = "uat_checklistbank";
    cfg.user = "clb";
    cfg.password = "%BBJu2MgstXJ";
    return cfg;
  }

  private static void dump2neo(NeoConfiguration neo, ClbConfiguration clb, UUID datasetKey, boolean nubRanksOnly) throws Exception {
    LOG.info("Dump dataset {} from postgres to local neo", datasetKey);
    ClbSource src = new ClbSource(clb, datasetKey, "Checklist " + datasetKey);
    src.setNeoRepository(neo.neoRepository);
    src.init(true, nubRanksOnly);
    LOG.info("Postrges dumped");
  }

  private static void dump2file(NeoConfiguration cfg, UUID datasetKey, GraphFormat format) throws Exception {
    File f = new File("/Users/markus/Desktop/repo/tree-"+datasetKey+"."+format.suffix);
    LOG.info("Dump local neo db {} to {} file {}", datasetKey, format, f.getAbsolutePath());
    UsageDao dao = UsageDao.persistentDao(cfg, datasetKey, true, null, false);
    try (Transaction tx = dao.beginTx()) {
       dao.printTree(new BufferedWriter(new FileWriter(f)), GraphFormat.TAB, true);
    }
    dao.close();
    LOG.info("Neo dumped");
  }

  public static void main(String[] args) throws Exception {
    //dump2neo(neo(), uat(), UUID.fromString("99948a8b-63b2-41bf-9d10-6e007e967789"), false);
    dump2file(neo(), UUID.fromString("99948a8b-63b2-41bf-9d10-6e007e967789"), GraphFormat.TAB);
  }
}