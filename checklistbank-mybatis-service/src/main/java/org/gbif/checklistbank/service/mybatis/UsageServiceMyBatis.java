package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageMapper;
import org.gbif.checklistbank.service.mybatis.postgres.IntArrayPgWriter;

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import com.google.inject.Inject;
import com.zaxxer.hikari.pool.ProxyConnection;
import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the UsageService using MyBatis.
 * All PagingResponses will not have the count set as it can be too costly sometimes.
 */
public class UsageServiceMyBatis implements UsageService {

  private static final Logger LOG = LoggerFactory.getLogger(UsageServiceMyBatis.class);

  private final NameUsageMapper mapper;
  private final UsageMapper usageMapper;

  @Inject
  private DataSource ds;

  @Inject
  UsageServiceMyBatis(NameUsageMapper mapper, UsageMapper usageMapper) {
    this.mapper = mapper;
    this.usageMapper = usageMapper;
  }

  @Override
  public List<Integer> listAll() {
    try (Connection con = ds.getConnection()){
      ProxyConnection hikari = (ProxyConnection) con;
      PGConnection pgcon = hikari.unwrap(PGConnection.class);
      IntArrayPgWriter intMapper = new IntArrayPgWriter();
      pgcon.getCopyAPI().copyOut("copy (SELECT id FROM name_usage WHERE deleted IS NULL) TO STDOUT WITH NULL '' ", intMapper);
      return intMapper.result();
    } catch (Exception e) {
      LOG.error("Failed to load all usage ids", e);
      throw new RuntimeException("Exception while loading usage ids", e);
    }
  }

  @Override
  public Integer maxUsageKey(UUID datasetKey) {
    return mapper.maxUsageKey(datasetKey);
  }

  @Override
  public List<NameUsage> listRange(int usageKeyStart, int usageKeyEnd) {
    return mapper.listRange(usageKeyStart, usageKeyEnd);
  }

  @Override
  public List<Integer> listParents(int usageKey) {
    return usageMapper.listParents(usageKey);
  }

  @Override
  public List<Integer> listOldUsages(UUID datasetKey, Date before) {
    return usageMapper.listByDatasetAndDate(datasetKey, before);
  }

}
