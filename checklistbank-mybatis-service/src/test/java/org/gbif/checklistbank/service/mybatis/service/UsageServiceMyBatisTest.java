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

import org.gbif.checklistbank.service.UsageService;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class UsageServiceMyBatisTest extends MyBatisServiceITBase {

  private final UsageService service;

  @Autowired
  public UsageServiceMyBatisTest(DataSource dataSource, UsageService usageService) {
    super(dataSource);
    this.service = usageService;
  }

  @Test
  public void testlistAll() {
    List<Integer> squirrels = service.listAll();
    assertEquals(46, squirrels.size());
  }

  @Test
  public void testlistParents() {
    List<Integer> squirrels = service.listParents(100000007);
    assertEquals(8, squirrels.size());
  }
}
