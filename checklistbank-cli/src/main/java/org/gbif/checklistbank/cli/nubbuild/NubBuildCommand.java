package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

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
    }

}
