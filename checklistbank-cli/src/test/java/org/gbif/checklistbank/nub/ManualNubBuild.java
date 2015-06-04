package org.gbif.checklistbank.nub;

import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;

import java.io.File;

public class ManualNubBuild {

  public static void main(String[] args) {
    // UAT CONFIGS without nub matching
    NubConfiguration cfg = new NubConfiguration();
    cfg.registry.wsUrl = "http://api.gbif-uat.org/v1";
    cfg.matching.matchWsUrl = null;
    cfg.neo.neoRepository = new File("/Users/markus/Desktop/repo");
    cfg.clb.serverName="pg1.gbif-uat.org";
    cfg.clb.databaseName="checklistbank";
    cfg.clb.user ="clb";
    cfg.clb.password="%BBJu2MgstXJ";

    NubBuilder nb = NubBuilder.create(cfg);
    nb.run();
  }
}