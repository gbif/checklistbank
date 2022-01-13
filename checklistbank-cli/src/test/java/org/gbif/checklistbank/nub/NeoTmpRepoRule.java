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
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class NeoTmpRepoRule implements TestRule {
    public final NeoConfiguration cfg;

    public NeoTmpRepoRule() {
        this.cfg = new NeoConfiguration();
        cfg.neoRepository = new File(FileUtils.getTempDirectory(), "clb");
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    private void before() throws IOException {
        if (cfg.neoRepository.exists()) {
            FileUtils.cleanDirectory(cfg.neoRepository);
        } else {
            cfg.neoRepository.mkdirs();
        }
    }

    private void after() {
        FileUtils.deleteQuietly(cfg.neoRepository);
    }
}
