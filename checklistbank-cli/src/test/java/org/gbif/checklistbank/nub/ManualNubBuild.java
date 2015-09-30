package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.lookup.IdLookupPassThru;
import org.gbif.checklistbank.nub.source.ClbSourceList;

import java.io.File;
import java.net.URI;

import com.yammer.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualNubBuild {
    private static final Logger LOG = LoggerFactory.getLogger(ManualNubBuild.class);

    private static NubConfiguration local() {
        NubConfiguration cfg = new NubConfiguration();
        cfg.autoImport = false;
        cfg.registry.wsUrl = "http://api.gbif-uat.org/v1";
        cfg.neo.neoRepository = new File("/Users/markus/Desktop/repo");
        cfg.neo.cacheType = NeoConfiguration.CacheType.SOFT;
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

    private static void build(NubConfiguration cfg) {
        LOG.info("Build new nub");
        MetricRegistry registry = new MetricRegistry("nub-build");
        UsageDao dao = UsageDao.persistentDao(cfg.neo, Constants.NUB_DATASET_KEY, registry, true);
        NubBuilder builder = NubBuilder.create(dao, ClbSourceList.create(cfg), new IdLookupPassThru(), 1000);
        builder.run();
        dao.close();
        LOG.info("New backbone ready");
    }

    public static void main(String[] args) {
        build(uat());
    }
}