package org.gbif.checklistbank.cli.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class ZookeeperUtils {

  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final Logger LOG = LoggerFactory.getLogger(ZookeeperUtils.class);
  private static final Joiner JOINER = Joiner.on('/').skipNulls();

  public static final String FINISHED_CRAWLING = "finishedCrawling";
  public static final String FINISHED_REASON = "finishedReason";
  public static final String PROCESS_STATE_CHECKLIST = "processState/checklist";
  public static final String PAGES_FRAGMENTED_SUCCESSFUL = "pagesFragmented/successful";
  public static final String PAGES_FRAGMENTED_ERROR = "pagesFragmented/error";

  private static final String CRAWL_INFO = "crawls";
  private final CuratorFramework curator;

  public ZookeeperUtils(CuratorFramework curator) {
    this.curator = curator;
  }

  /**
   * Helper method to retrieve a path under the {@link #CRAWL_INFO} node.
   *
   * @param uuid of the dataset to get the path for
   * @param path if null we retrieve the path of the {@link #CRAWL_INFO} node itself otherwise we append this path
   */
  public static String getCrawlInfoPath(UUID uuid, @Nullable String path) {
    checkNotNull(uuid, "uuid can't be null");
    return JOINER.join(CRAWL_INFO, uuid, path);
  }

  /**
   * Builds a "/" separated path out of path elements. The returned string will not have a "/" as the first and last
   * character.
   *
   * @param paths to concatenate
   */
  public static String buildPath(String... paths) {
    return JOINER.join(paths);
  }

  public void createOrUpdate(String crawlPath, byte[] data) {
    try {
      Stat stat = curator.checkExists().forPath(crawlPath);
      if (stat == null) {
        curator.create().creatingParentsIfNeeded().forPath(crawlPath, data);
      } else {
        curator.setData().forPath(crawlPath, data);
      }
    } catch (Exception e1) {
      LOG.error("Exception while updating ZooKeeper", e1);
    }
  }

  public void createOrUpdate(UUID datasetKey, String subPath, byte[] data) {
    createOrUpdate(getCrawlInfoPath(datasetKey, subPath), data);
  }

  public void createOrUpdate(UUID datasetKey, String subPath, Enum<?> data) {
    createOrUpdate(datasetKey, subPath, data.name().getBytes(Charsets.UTF_8));
  }

  /**
   * Updates a node in ZooKeeper saving the current date in time in there.
   *
   * @param datasetKey designates the first bit of the path to update
   * @param path       the path to update within the dataset node
   */
  public void updateDate(UUID datasetKey, String path) {
    String crawlPath = getCrawlInfoPath(datasetKey, path);
    Date date = new Date();

    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    byte[] data = dateFormat.format(date).getBytes(Charsets.UTF_8);
    createOrUpdate(crawlPath, data);
  }

  public byte[] getData(UUID datasetKey, String path) throws Exception {
    String crawlPath = getCrawlInfoPath(datasetKey, path);
    return curator.getData().forPath(crawlPath).clone();
  }

  public Date getDate(UUID datasetKey, String path) {
    try {
      byte[] data = getData(datasetKey, path);
      SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      return dateFormat.parse(new String(data));

    } catch (Exception e) {
      LOG.error("Exception while getting date from ZooKeeper", e);
      return null;
    }
  }

  public DistributedAtomicLong getCounter(UUID datasetKey, String path) {
    return new DistributedAtomicLong(curator, getCrawlInfoPath(datasetKey, path), new RetryNTimes(5, 1000));
  }

  public void updateCounter(UUID datasetKey, String path, long value) {
    DistributedAtomicLong dal = getCounter(datasetKey, path);
    try {
      AtomicValue<Long> atom = dal.trySet(value);
      // we must check if the operation actually succeeded
      // see https://github.com/Netflix/curator/wiki/Distributed-Atomic-Long
      if (!atom.succeeded()) {
        LOG.error("Failed to update counter {} for dataset {}", path, datasetKey);
      }
    } catch (Exception e) {
      LOG.error("Failed to update counter {} for dataset {}", path, datasetKey, e);
    }
  }

}
