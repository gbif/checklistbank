/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.UsageMapper;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.IntArrayPgWriter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.pool.ProxyConnection;

/**
 * Implements the UsageService using MyBatis. All PagingResponses will not have the count set as it
 * can be too costly sometimes.
 */
@Service
public class UsageServiceMyBatis implements UsageService {

  private static final Logger LOG = LoggerFactory.getLogger(UsageServiceMyBatis.class);

  private final NameUsageMapper mapper;
  private final UsageMapper usageMapper;
  private DataSource ds;

  @Autowired
  UsageServiceMyBatis(NameUsageMapper mapper, UsageMapper usageMapper, DataSource ds) {
    this.mapper = mapper;
    this.usageMapper = usageMapper;
    this.ds = ds;
  }

  @Override
  public List<Integer> listAll() {
    try (Connection con = ds.getConnection();
        IntArrayPgWriter intMapper = new IntArrayPgWriter()) {
      ProxyConnection hikari = (ProxyConnection) con;
      PGConnection pgcon = hikari.unwrap(PGConnection.class);
      pgcon
          .getCopyAPI()
          .copyOut(
              "copy (SELECT id FROM name_usage WHERE deleted IS NULL) TO STDOUT WITH NULL '' ",
              intMapper);
      return intMapper.result();
    } catch (IOException | SQLException e) {
      throw new RuntimeException("Exception while loading usage ids", e);
    }
  }

  @Override
  public Integer maxUsageKey(UUID datasetKey) {
    return mapper.maxUsageKey(datasetKey);
  }

  @Override
  public Integer minUsageKey(UUID datasetKey) {
    return mapper.minUsageKey(datasetKey);
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
