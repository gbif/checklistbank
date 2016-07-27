package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ParsedNameMapper;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NubNameUpdater implements ResultHandler<NameUsage> {
  private static final Logger LOG = LoggerFactory.getLogger(NubNameUpdater.class);

  private final NameUsageMapper usageMapper;
  private final ParsedNameMapper nameMapper;
  private final ParsedNameService pNameService;

  private int counter = 0;
  private int updCounter = 0;

  public NubNameUpdater(NameUsageMapper usageMapper, ParsedNameMapper nameMapper, ParsedNameService pNameService) {
    this.usageMapper = usageMapper;
    this.nameMapper = nameMapper;
    this.pNameService = pNameService;
  }

  @Override
  public void handleResult(ResultContext<? extends NameUsage> resultContext) {
    NameUsage u = resultContext.getResultObject();
    ParsedName pn = nameMapper.getByName(u.getScientificName());
    counter++;
    if (pn.isParsableType() && !pn.getScientificName().equalsIgnoreCase(pn.canonicalNameComplete())) {
      // update the name table
      pn.setScientificName(pn.canonicalNameComplete());
      pn = pNameService.createOrGet(pn);
      // rewire usage
      if (pn.getKey() == null) {
        LOG.error("Updating usage {} {} failed", u.getKey(), u.getScientificName());

      } else {
        usageMapper.updateName(u.getKey(), pn.getKey());
        updCounter++;
        LOG.debug("Updating usage {}: {} -> {}", u.getKey(), u.getScientificName(), pn.getScientificName());
        if (updCounter % 1000 == 0) {
          LOG.info("Updated {} inconsistent names out of {}", updCounter, counter);
        }
      }
    }
  }

  public int getCounter() {
    return counter;
  }

  public int getUpdCounter() {
    return updCounter;
  }
}
