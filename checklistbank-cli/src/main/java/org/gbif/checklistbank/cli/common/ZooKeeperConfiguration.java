package org.gbif.checklistbank.cli.common;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Strings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import java.io.IOException;

/**
 * A configuration class which can be used to get all the details needed to persistent a connection to ZooKeeper needed by
 * the Curator Framework.
 * It provides a convenience method ({@link #getCuratorFramework()} ()} to actually get a {@link org.apache.curator.framework.CuratorFramework}
 * object when populated fully.
 */
@SuppressWarnings("PublicField")
public class ZooKeeperConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperConfiguration.class);

  @Parameter(
    names = "--zk-connection-string",
    description = "The connection string to connect to ZooKeeper. Keep empty if zookeeper is not used")
  @Nullable
  public String connectionString;

  @Parameter(
    names = "--zk-namespace",
    description = "The namespace in ZooKeeper under which all data lives")
  @Nullable
  public String namespace;

  @Parameter(
    names = "--zk-sleep-time",
    description = "Initial amount of time to wait between retries in ms")
  @Min(1)
  public int baseSleepTime = 1000;

  @Parameter(
    names = "--zk-max-retries",
    description = "Max number of times to retry")
  @Min(1)
  public int maxRetries = 10;

  /**
   * This method returns a connection object to ZooKeeper with the provided settings and creates and starts a {@link
   * org.apache.curator.framework.CuratorFramework}. These settings are not validated in this method so only call it when the object has been
   * validated.
   *
   * @return started CuratorFramework
   *
   * @throws java.io.IOException if connection fails
   */
  @JsonIgnore
  public CuratorFramework getCuratorFramework() throws IOException {
    LOG.info("Connecting to zookeeper at {} within namespace {}", connectionString, namespace);
    CuratorFramework curator = CuratorFrameworkFactory.builder()
        .namespace(namespace)
        .retryPolicy(new ExponentialBackoffRetry(baseSleepTime, maxRetries))
        .connectString(connectionString)
        .build();
    curator.start();
    return curator;
  }

  public boolean isConfigured() {
    return !Strings.isStringEmpty(namespace) && !Strings.isStringEmpty(connectionString);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("connectionString", connectionString).add("namespace", namespace)
      .add("baseSleepTime", baseSleepTime).add("maxRetries", maxRetries).toString();

  }
}
