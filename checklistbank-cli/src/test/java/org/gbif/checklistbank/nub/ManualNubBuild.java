package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.source.ClbSourceList;
import org.gbif.nub.lookup.straight.IdLookupPassThru;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

public class ManualNubBuild {
    private static final Logger LOG = LoggerFactory.getLogger(ManualNubBuild.class);

    private static NubConfiguration local() {
        NubConfiguration cfg = new NubConfiguration();
        cfg.autoImport = false;
        cfg.registry.wsUrl = "http://api.gbif-uat.org/v1";
        cfg.neo.neoRepository = new File("/Users/markus/Desktop/repo");
        cfg.neo.mappedMemory = 1024;
        cfg.clb.serverName = "localhost";
        cfg.clb.databaseName = "clb";
        cfg.clb.user = "postgres";
        cfg.clb.password = "pogo";
        return cfg;
    }

    private static NubConfiguration uat() {
        NubConfiguration cfg = local();
        cfg.registry.wsUrl = "http://api.gbif.org/v1";
        cfg.clb.serverName = "pg1.gbif-uat.org";
        cfg.clb.databaseName = "uat_checklistbank";
        cfg.clb.user = "clb";
        cfg.clb.password = "%BBJu2MgstXJ";
        cfg.sourceList = URI.create("https://dl.dropboxusercontent.com/u/457027/nub-sources.tsv");
        return cfg;
    }

    private static void build(NubConfiguration cfg, UUID ... sources) {
        LOG.info("Build new nub");
        UsageDao dao = UsageDao.create(cfg.neo, Constants.NUB_DATASET_KEY);
        NubBuilder builder;
        if (sources == null) {
            builder = NubBuilder.create(dao, ClbSourceList.create(cfg), new IdLookupPassThru(), 1000, 1000);
        } else {
            builder = NubBuilder.create(dao, ClbSourceList.create(cfg, Arrays.asList(sources)), new IdLookupPassThru(), 1000, 1000);
        }
        builder.run();
        dao.close();
        LOG.info("New backbone ready");
    }

    public static void main(String[] args) {
        build(uat(), UUID.fromString("bf3db7c9-5e5d-4fd0-bd5b-94539eaf9598"));
    }
}