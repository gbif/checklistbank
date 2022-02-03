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
package org.gbif.checklistbank.nub;

import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.cli.common.SpringContextBuilder;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.validation.NubAssertions;
import org.gbif.checklistbank.service.mybatis.service.NameUsageServiceMyBatis;
import org.gbif.checklistbank.utils.ClbConfigurationUtils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NubAssertionsTest {

  @Test
  public void testValidate() throws Exception {
    UsageDao dao = UsageDao.temporaryDao(10);
    NubDb db = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
    NubAssertions ass = new NubAssertions(db);
    assertFalse(ass.validate());
  }

  @Test
  @Disabled("manual test only")
  public void validatePostgresNub() throws Exception {
    ClbConfiguration cfg = ClbConfigurationUtils.uat();

    ApplicationContext ctx =
        SpringContextBuilder.create()
            .withClbConfiguration(cfg)
            .withComponents(NameUsageServiceMyBatis.class)
            .build();

    NubAssertions ass = new NubAssertions(ctx.getBean(NameUsageServiceMyBatis.class));

    assertTrue(ass.validate());
  }
}
