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

import org.gbif.checklistbank.cli.common.NeoConfiguration;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class NeoTmpRepoRule implements AfterEachCallback, BeforeEachCallback {
    public final NeoConfiguration cfg;

    public NeoTmpRepoRule() {
        this.cfg = new NeoConfiguration();
        cfg.neoRepository = new File(FileUtils.getTempDirectory(), "clb");
    }


    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        FileUtils.deleteQuietly(cfg.neoRepository);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        if (cfg.neoRepository.exists()) {
            FileUtils.cleanDirectory(cfg.neoRepository);
        } else {
            cfg.neoRepository.mkdirs();
        }
    }
}
