package org.gbif.checklistbank.service.mybatis;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.model.Usage;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;

/**
 * Implements the NameUsageService using MyBatis.
 * All PagingResponses will not have the count set as it can be too costly sometimes.
 */
public class DatasetImportServiceMyBatis implements DatasetImportService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetImportServiceMyBatis.class);

  private final NameUsageMapper mapper;
  private final UsageMapper usageMapper;
  private final NubRelMapper nubRelMapper;
  private final ParsedNameMapper parsedNameMapper;

  @Inject
  private DataSource ds;

  @Inject
  DatasetImportServiceMyBatis(NameUsageMapper mapper, UsageMapper usageMapper, ParsedNameMapper parsedNameMapper,
    NubRelMapper nubRelMapper) {
    this.mapper = mapper;
    this.parsedNameMapper = parsedNameMapper;
    this.usageMapper = usageMapper;
    this.nubRelMapper = nubRelMapper;
  }

  @Override
  public void insertUsages(UUID datasetKey, Iterator<Usage> iter) {
    final int BATCH_SIZE = 1000;

    int batchCounter = 1;
    List<Usage> batch = Lists.newArrayList();
    while (iter.hasNext()) {
      batch.add(iter.next());
      if (batch.size() % BATCH_SIZE == 0) {
        LOG.debug("Insert nub usage batch {}", batchCounter);
        insertUsageBatch(datasetKey, batch);
        batchCounter++;
        batch.clear();
      }
    }
    LOG.debug("Insert last nub usage batch {}", batchCounter);
    insertUsageBatch(datasetKey, batch);
  }

    @Override
    public Integer syncUsage(NameUsageContainer usage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBasionym(Integer usageKey, Integer basionymKey) {
        throw new UnsupportedOperationException();
    }

    @Transactional(
      executorType = ExecutorType.BATCH,
      isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
      exceptionMessage = "Something went wrong while inserting nub relations batch for dataset {0}"
  )
  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    nubRelMapper.deleteByDataset(datasetKey);
    for (Map.Entry<Integer, Integer> entry : relations.entrySet()) {
      nubRelMapper.insert(datasetKey, entry.getKey(), entry.getValue());
    }
  }

  @Transactional(
      executorType = ExecutorType.BATCH,
      isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED,
      exceptionMessage = "Something went wrong while inserting usage batch for dataset {0}"
  )
  private void insertUsageBatch(UUID datasetKey, List<Usage> usages) {
    for (Usage u : usages) {
      usageMapper.insert(datasetKey, u);
    }
  }

  @Override
  public void deleteDataset(UUID datasetKey) {
    LOG.info("Deleting entire dataset {}", datasetKey);
    usageMapper.deleteByDataset(datasetKey);
  }

    @Override
    public void deleteOldUsages(UUID datasetKey, Date before) {
        throw new UnsupportedOperationException();
    }


}
