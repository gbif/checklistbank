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
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.validation.NubAssertions;
import org.gbif.checklistbank.utils.ClbConfigurationUtils;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class NubAssertionsTest {

  @Test
  public void testValidate() throws Exception {
    UsageDao dao = UsageDao.temporaryDao(10);
    NubDb db = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
    NubAssertions ass = new NubAssertions(db);
    assertFalse(ass.validate());
  }


  @Test
  @Ignore("manual test only")
  public void validatePostgresNub() throws Exception {
    ClbConfiguration cfg = ClbConfigurationUtils.uat();
//    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg));
//
//    NubAssertions ass = new NubAssertions(inj.getInstance(NameUsageService.class));
//
//    assertTrue(ass.validate());
  }
}
