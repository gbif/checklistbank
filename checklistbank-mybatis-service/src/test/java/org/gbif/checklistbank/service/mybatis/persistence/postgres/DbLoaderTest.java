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
package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.checklistbank.service.mybatis.persistence.ChecklistBankMyBatisConfiguration;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Simply checks no exceptions occurr when loading the standard squirrelts dataset.
 */
public class DbLoaderTest {

    private final AnnotationConfigApplicationContext
      ctx = new AnnotationConfigApplicationContext(ChecklistBankMyBatisConfiguration.class);

    @Test
    public void testLoad() throws Exception {
        Properties properties = PropertiesUtil.loadProperties(ClbDbTestRule.DEFAULT_PROPERTY_FILE);
        DbLoader.load(ctx.getBean(DataSource.class).getConnection(), "squirrels", true);
    }
}