package org.gbif.checklistbank.exporter;

import java.io.File;
import java.net.URI;

import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class HdfsConfiguration {

  private static final String DEFAULT_FS = "file:///";

  @SneakyThrows
  @Bean
  public FileSystem hdfsFileSystem(
      @Value("${hdfsSitePath}") String hdfsSitePath,
      @Value("${coreSitePath}") String coreSitePath
  ) {
    HdfsConfigs hdfsConfigs = HdfsConfigs.of(hdfsSitePath, coreSitePath);
    String hdfsPrefixToUse = getHdfsPrefix(hdfsConfigs);
    String corePrefixToUse = getHdfsPrefix(hdfsConfigs);

    String prefixToUse;
    if (!DEFAULT_FS.equals(hdfsPrefixToUse)) {
      prefixToUse = hdfsPrefixToUse;
    } else if (!DEFAULT_FS.equals(corePrefixToUse)) {
      prefixToUse = corePrefixToUse;
    } else {
      prefixToUse = hdfsPrefixToUse;
    }

    if (prefixToUse != null) {
      org.apache.hadoop.conf.Configuration config = getHdfsConfiguration(hdfsConfigs);
      return FileSystem.get(URI.create(prefixToUse), config);
    } else {
      throw new IllegalArgumentException("XML config is provided, but fs name is not found");
    }
  }

  /**
   * Creates an instances of a {@link org.apache.hadoop.conf.Configuration} using a xml HDFS configuration file.
   *
   * @param hdfsConfigs coreSiteConfig path to the hdfs-site.xml or core-site.xml
   * @return a {@link org.apache.hadoop.conf.Configuration} based on the provided config file
   */
  @SneakyThrows
  private static org.apache.hadoop.conf.Configuration getHdfsConfiguration(HdfsConfigs hdfsConfigs) {
    // check if the hdfs-site.xml is provided
    if (Strings.isNullOrEmpty(hdfsConfigs.getHdfsSiteConfig())
        || Strings.isNullOrEmpty(hdfsConfigs.getCoreSiteConfig())) {
      throw new IllegalArgumentException("Hdfs or core config values null or empty");
    }

    File hdfsSiteFile = new File(hdfsConfigs.getHdfsSiteConfig());
    File coreSiteFile = new File(hdfsConfigs.getCoreSiteConfig());
    if (hdfsSiteFile.exists()
        && hdfsSiteFile.isFile()
        && coreSiteFile.exists()
        && coreSiteFile.isFile()) {
      org.apache.hadoop.conf.Configuration config = new org.apache.hadoop.conf.Configuration(false);
      log.info("Using XML config found at {} and {}", hdfsSiteFile, coreSiteFile);
      config.addResource(hdfsSiteFile.toURI().toURL());
      config.addResource(coreSiteFile.toURI().toURL());
      return config;
    } else {
      log.warn(
          "XML config does not exist - {} or {}",
          hdfsConfigs.getHdfsSiteConfig(),
          hdfsConfigs.getCoreSiteConfig());
      throw new IllegalArgumentException("Hdfs or core config files do not exist");
    }
  }

  private static String getHdfsPrefix(HdfsConfigs hdfsConfigs) {
    String hdfsPrefixToUse = null;
    if (!Strings.isNullOrEmpty(hdfsConfigs.getHdfsSiteConfig())
        && !Strings.isNullOrEmpty(hdfsConfigs.getCoreSiteConfig())) {
      org.apache.hadoop.conf.Configuration hdfsSite = getHdfsConfiguration(hdfsConfigs);
      hdfsPrefixToUse = hdfsSite.get("fs.default.name");
      if (hdfsPrefixToUse == null) {
        hdfsPrefixToUse = hdfsSite.get("fs.defaultFS");
      }
    }
    return hdfsPrefixToUse;
  }

  @Getter
  @AllArgsConstructor(staticName = "of")
  private static class HdfsConfigs {

    private final String hdfsSiteConfig;
    private final String coreSiteConfig;
  }
}
