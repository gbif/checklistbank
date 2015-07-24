package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.GenericValidationReport;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.cli.deletion.DeleteService;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
@MetaInfServices(Command.class)
public class AdminCommand extends BaseCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AdminCommand.class);
    private static final String DWCA_SUFFIX = ".dwca";

    private final AdminConfiguration cfg = new AdminConfiguration();
    private MessagePublisher publisher;
    private ZookeeperUtils zkUtils;
    private DatasetService datasetService;
    private OrganizationService organizationService;
    private InstallationService installationService;
    private NetworkService networkService;
    private NodeService nodeService;
    private Iterable<Dataset> datasets;

    public AdminCommand() {
        super("admin");
    }

    @Override
    protected Object getConfigurationObject() {
        return cfg;
    }

    private void initRegistry() {
        Injector inj = cfg.registry.createRegistryInjector();
        datasetService = inj.getInstance(DatasetService.class);
        organizationService = inj.getInstance(OrganizationService.class);
        installationService = inj.getInstance(InstallationService.class);
        networkService = inj.getInstance(NetworkService.class);
        nodeService = inj.getInstance(NodeService.class);
    }

    private ZookeeperUtils zk() {
        if (zkUtils == null) {
            try {
                zkUtils = new ZookeeperUtils(cfg.zookeeper.getCuratorFramework());
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
        return zkUtils;
    }

    private void send(Message msg) throws IOException {
        if (publisher == null) {
            publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
        }
        publisher.send(msg);
    }

    @Override
    protected void doRun() {
        initRegistry();
        datasets = Iterables.datasets(cfg.key, cfg.type, datasetService, organizationService, installationService, networkService, nodeService);
        for (Dataset d : datasets) {
            try {
                LOG.info("{} {} dataset {}: {}", cfg.operation, d.getType(), d.getKey(), d.getTitle().replaceAll("\n", " "));
                switch (cfg.operation) {
                    case CLEANUP:
                        zk().delete(ZookeeperUtils.getCrawlInfoPath(d.getKey(), null));
                        LOG.info("Removed crawl {} from zookeeper", d.getKey());

                        // cleanup repo files
                        final File dwcaFile = new File(cfg.archiveRepository, d.getKey() + DWCA_SUFFIX);
                        FileUtils.deleteQuietly(dwcaFile);
                        File dir = cfg.archiveDir(d.getKey());
                        if (dir.exists() && dir.isDirectory()) {
                            FileUtils.deleteDirectory(dir);
                        }
                        LOG.info("Removed dwca files from repository {}", dwcaFile);

                        DeleteService.deleteStorageFiles(cfg.neo, d.getKey());
                        break;

                    case CRAWL:
                        send(new StartCrawlMessage(d.getKey()));
                        break;

                    case NORMALIZE:
                        if (!cfg.archiveDir(d.getKey()).exists()) {
                            LOG.info("Missing dwca file. Cannot normalize dataset {}", title(d));
                        } else {
                            // validation result is a fake valid checklist validation
                            send(new DwcaMetasyncFinishedMessage(d.getKey(), d.getType(),
                                            URI.create("http://fake.org"), 1, Maps.<String, UUID>newHashMap(),
                                            new DwcaValidationReport(d.getKey(),
                                                    new GenericValidationReport(1, true, Lists.<String>newArrayList(), Lists.<Integer>newArrayList()))
                                    )
                            );
                        }
                        break;

                    case IMPORT:
                        if (!cfg.neo.neoDir(d.getKey()).exists()) {
                            LOG.info("Missing neo4j directory. Cannot import dataset {}", title(d));
                        } else {
                            send(new ChecklistNormalizedMessage(d.getKey()));
                        }
                        break;

                    case ANALYZE:
                        send(new ChecklistSyncedMessage(d.getKey(), new Date(), 0, 0));
                        break;

                    default:
                        throw new UnsupportedOperationException();
                }

            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }


    private String title(Dataset d) {
        return d.getKey() + ": " + d.getTitle().replaceAll("\n", " ");
    }
}
