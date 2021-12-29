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
package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import lombok.SneakyThrows;

@EnableConfigurationProperties
@SpringBootApplication
public class NubMatchingServiceTestManual implements CommandLineRunner {

  private final NameUsageMatchingService matcher;

  @Autowired
  public NubMatchingServiceTestManual(NameUsageMatchingService matcher) {
    this.matcher = matcher;
  }

  public void testMatching() {
    LinneanClassification cl = new NameUsageMatch();
    // test identical
    matcher.match("Animalia", null, cl, true, true);
    matcher.match("Animals", null, cl, true, true);
    matcher.match("Insects", null, cl, true, true);
    cl.setKingdom("Animalia");
    matcher.match("Puma concolor", null, cl, true, true);
    cl.setKingdom("Plantae");
    matcher.match("Puma concolor", null, cl, true, true);
  }

  public static void main(String[] args) {
    SpringApplication.run(NubMatchingServiceTestManual.class, args);
  }

  @Override
  @SneakyThrows
  public void run(String... args) {
    testMatching();
  }
}
