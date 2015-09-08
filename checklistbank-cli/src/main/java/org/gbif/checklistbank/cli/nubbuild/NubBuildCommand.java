package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;

import java.io.IOException;

import com.google.common.base.Throwables;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MetaInfServices(Command.class)
public class NubBuildCommand extends BaseCommand {
    private static final Logger LOG = LoggerFactory.getLogger(NubBuildCommand.class);
    private final NubConfiguration cfg = new NubConfiguration();

    public NubBuildCommand() {
        super("nub-build");
    }

    @Override
    protected Object getConfigurationObject() {
        return cfg;
    }

    @Override
    protected void doRun() {
        NubBuilder builder = NubBuilder.create(cfg);
        builder.run();

        if (cfg.autoImport) {
            try {
                MessagePublisher publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
                publisher.send(new ChecklistNormalizedMessage(Constants.NUB_DATASET_KEY));
                LOG.info("Sending ChecklistNormalizedMessage for backbone dataset {}", Constants.NUB_DATASET_KEY);
                publisher.close();
            } catch (IOException e) {
                Throwables.propagate(e);
            }

        }
    }

}
