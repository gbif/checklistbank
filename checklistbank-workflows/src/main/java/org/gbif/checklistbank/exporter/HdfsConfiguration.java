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

import com.google.common.base.Strings;
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
      if (Strings.isNullOrEmpty(fileName)) {
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
