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
package org.gbif.checklistbank.elasticsearch;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.parser.ParserException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Configuration settings for Checklistbank batch indexing.
 */
public class EsBackfillConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchConfiguration.class);

  //Jackson/YAML mapper to read configuration settings from a YAML file.
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private int indexingPartitions = 80;

  private String sourceDirectory;

  private ElasticsearchConfiguration elasticsearch;

  /**
   * Loads the configuration YAML file into a BackfillConfiguration instance.
   */
  public static EsBackfillConfiguration loadFromFile(String fileName) {
    File file = new File(fileName);
    if (!file.exists()) {
      String message = "Error reading configuration file [" + fileName + "] because it does not exist";
      LOG.error(message);
      throw new IllegalArgumentException(message);
    }
    EsBackfillConfiguration esBackfillConfiguration = new EsBackfillConfiguration();
    ObjectReader reader = MAPPER.readerForUpdating(esBackfillConfiguration);
    try {
      reader.readValue(file);
    } catch (IOException | ParserException e) {
      String message = "Error reading configuration file [" + fileName + "]";
      LOG.error(message);
      throw new IllegalArgumentException(message, e);
    }
    return esBackfillConfiguration;
  }
}
