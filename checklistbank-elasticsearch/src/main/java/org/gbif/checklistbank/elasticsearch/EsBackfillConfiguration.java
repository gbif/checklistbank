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

import org.gbif.checklistbank.utils.PropertiesUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.parser.ParserException;

import static org.gbif.checklistbank.elasticsearch.ElasticsearchConfiguration.DEFAULT_CONNECTION_REQUEST_TIMEOUT;
import static org.gbif.checklistbank.elasticsearch.ElasticsearchConfiguration.DEFAULT_CONNECTION_TIMEOUT;
import static org.gbif.checklistbank.elasticsearch.ElasticsearchConfiguration.DEFAULT_SOCKET_TIMEOUT;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Configuration settings for Checklistbank batch indexing. */
public class EsBackfillConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(EsBackfillConfiguration.class);

  private static final int DEFAULT_INDEXING_PARTITIONS = 80;

  // Jackson/YAML mapper to read configuration settings from a YAML file.
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private int indexingPartitions = DEFAULT_INDEXING_PARTITIONS;

  private String sourceDirectory;

  private ElasticsearchConfiguration elasticsearch;

  /** Loads the configuration YAML file into a BackfillConfiguration instance. */
  public static EsBackfillConfiguration loadFromFile(String fileName) {
    File file = new File(fileName);
    if (!file.exists()) {
      String message =
          "Error reading configuration file [" + fileName + "] because it does not exist";
      LOG.error(message);
      throw new IllegalArgumentException(message);
    }

    try {
      if (fileName.endsWith(".properties")) {
        return readFromProperties(fileName);
      } else {
        // read from YAML
        EsBackfillConfiguration esBackfillConfiguration = new EsBackfillConfiguration();
        ObjectReader reader = MAPPER.readerForUpdating(esBackfillConfiguration);
        reader.readValue(file);
        return esBackfillConfiguration;
      }
    } catch (IOException | ParserException e) {
      String message = "Error reading configuration file [" + fileName + "]";
      LOG.error(message);
      throw new IllegalArgumentException(message, e);
    }
  }

  private static EsBackfillConfiguration readFromProperties(String fileName) throws IOException {
    EsBackfillConfiguration esBackfillConfiguration = new EsBackfillConfiguration();
    Properties props = new Properties();
    try (FileReader reader = new FileReader(fileName)) {
      props.load(reader);
      esBackfillConfiguration.indexingPartitions =
          PropertiesUtils.getIntProp(props, "indexingPartitions", DEFAULT_INDEXING_PARTITIONS);
      esBackfillConfiguration.sourceDirectory = props.getProperty("sourceDirectory");

      ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
      esBackfillConfiguration.elasticsearch = elasticsearchConfiguration;
      elasticsearchConfiguration.setAlias(props.getProperty("elasticsearch.alias"));
      elasticsearchConfiguration.setHost(props.getProperty("elasticsearch.host"));
      elasticsearchConfiguration.setIndex(props.getProperty("elasticsearch.index"));
      elasticsearchConfiguration.setConnectionTimeOut(
          PropertiesUtils.getIntProp(props, "elasticsearch.connectionTimeOut", DEFAULT_CONNECTION_TIMEOUT));
      elasticsearchConfiguration.setSocketTimeOut(
          PropertiesUtils.getIntProp(props, "elasticsearch.socketTimeOut", DEFAULT_SOCKET_TIMEOUT));
      elasticsearchConfiguration.setConnectionRequestTimeOut(
          PropertiesUtils.getIntProp(
              props, "elasticsearch.connectionRequestTimeOut", DEFAULT_CONNECTION_REQUEST_TIMEOUT));
      elasticsearchConfiguration.setNumberOfShards(
          PropertiesUtils.getIntProp(props, "elasticsearch.numberOfShards", 1));
      elasticsearchConfiguration.setMappingsFile(props.getProperty("elasticsearch.mappingsFile"));
      elasticsearchConfiguration.setSettingsFile(props.getProperty("elasticsearch.settingsFile"));
    }

    return esBackfillConfiguration;
  }
}
