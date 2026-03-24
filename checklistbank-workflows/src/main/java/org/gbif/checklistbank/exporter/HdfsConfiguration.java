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
package org.gbif.checklistbank.exporter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.function.Function;

import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class HdfsConfiguration {

  @SneakyThrows
  @Bean
  public FileSystem hdfsFileSystem(
      @Value("${hdfsSitePath}") String hdfsSitePath,
      @Value("${coreSitePath}") String coreSitePath
  ) {

    org.apache.hadoop.conf.Configuration config = getHdfsConfiguration(hdfsSitePath, coreSitePath);
    String prefixToUse = getHdfsPrefix(config);

    return FileSystem.get(URI.create(prefixToUse), config);
  }

  /**
   * Creates an instances of a {@link org.apache.hadoop.conf.Configuration} using a xml HDFS configuration file.
   *
   * @param coreSiteConfig path to the core-site.xml
   * @param hdfsSiteConfig path to the hdfs-site.xml
   * @return a {@link org.apache.hadoop.conf.Configuration} based on the provided config file
   */
  @SneakyThrows
  private static org.apache.hadoop.conf.Configuration getHdfsConfiguration(String hdfsSiteConfig,
      String coreSiteConfig) {

    // check if the hdfs-site.xml is provided
    Function<String, URL> getFileAsUrl = fileName -> {
      if (fileName == null || fileName.isBlank()) {
        throw new IllegalArgumentException(fileName + " value null or empty");
      }
      File file = new File(fileName);
      if (!file.exists() || !file.isFile()) {
        throw new IllegalArgumentException(fileName + " doesn't exists");
      }
      try {
        return file.toURI().toURL();
      } catch (MalformedURLException ex) {
        throw new IllegalArgumentException(ex);
      }
    };

    org.apache.hadoop.conf.Configuration config = new org.apache.hadoop.conf.Configuration(false);
    log.info("Using XML config found at {} and {}", hdfsSiteConfig, coreSiteConfig);
    config.addResource(getFileAsUrl.apply(hdfsSiteConfig));
    config.addResource(getFileAsUrl.apply(coreSiteConfig));
    return config;

  }

  private static String getHdfsPrefix(org.apache.hadoop.conf.Configuration hdfsSite) {
    String hdfsPrefixToUse = hdfsSite.get("fs.defaultFS");
    Objects.requireNonNull(hdfsPrefixToUse, "XML config is provided, but fs.defaultFS is not found");

    log.info("HDFS Prefix - {}", hdfsPrefixToUse);
    return hdfsPrefixToUse;
  }
}
