package org.gbif.checklistbank.verbatim;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.mapper.RawUsageMapper;
import org.gbif.checklistbank.utils.VerbatimNameUsageMapper;
import org.gbif.checklistbank.utils.VerbatimNameUsageMapperKryo;
import org.gbif.checklistbank.utils.VerbatimNameUsageMapperSmile;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.kohsuke.MetaInfServices;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates verbatim smile data into the new kryo format.
 */
@MetaInfServices(Command.class)
public class UpdateVerbatimCommand extends BaseCommand {
  private static final Logger LOG = LoggerFactory.getLogger(UpdateVerbatimCommand.class);

  private final ClbConfiguration cfg = new ClbConfiguration();
  private RawUsageMapper rawUsageMapper;
  private VerbatimNameUsageMapper smileMapper = new VerbatimNameUsageMapperSmile();
  private VerbatimNameUsageMapper kryoMapper = new VerbatimNameUsageMapperKryo();
  private int counter = 1;

  public UpdateVerbatimCommand() {
    super("updateVerbatim");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  @Override
  protected void doRun() {
    InternalChecklistBankServiceMyBatisModule mod = cfg.createServiceModule();
    Injector inj = Guice.createInjector(mod);
    UsageService usageService = inj.getInstance(UsageService.class);
    rawUsageMapper = inj.getInstance(RawUsageMapper.class);

    LOG.info("Getting list of all usage keys ...");
    for (List<Integer> batch : Lists.partition(usageService.listAll(), 1000)) {
      update(batch);
    }
  }

  @Transactional
  private void update(List<Integer> batch) {
    LOG.info("Update batch {}", counter++);
    for (Integer key : batch) {
      update(key);
    }
  }

  private void update(int key) {
    try {
      RawUsage raw = rawUsageMapper.get(key);
      if (raw != null) {
        VerbatimNameUsage v = smileMapper.read(raw.getData());
        RawUsage rawKryo = new RawUsage();
        raw.setUsageKey(key);
        raw.setData(kryoMapper.write(v));
        rawUsageMapper.update(rawKryo);
      }
    } catch (Exception e) {
      LOG.error("Failed to update usage " + key, e);
    }
  }

}
