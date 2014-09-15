package org.gbif.nub.build;

import org.gbif.file.TabWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubReporter {
  private static final Logger LOG = LoggerFactory.getLogger(NubReporter.class);

  private final File dir;
  // we keep an additional counter for all created nub usages so its quickly accesible
  private int createdTotal = 0;
  private Map<UUID, DatasetReport> reports = Maps.newHashMap();
  private final TabWriter newIdWriter;

  @Inject
  public NubReporter(File reportDir) throws FileNotFoundException {
    this.dir = reportDir;
    if (!dir.exists()) {
      try {
        FileUtils.forceMkdir(dir);
      } catch (IOException e) {
        throw new IllegalArgumentException("Cannot insert reporting dir", e);
      }
    }
    newIdWriter = new TabWriter(new FileOutputStream(new File(reportDir, "new-ids.tab")));
  }

  public int getCreatedTotal() {
    return createdTotal;
  }

  public class DatasetReport {
    private int ignored;
    private int created; // number of nub usages// based on a usage from
    private int updated;
    private int newIds; // number of nub usages with new ids, included in created

    public int getTotal() {
      return ignored + created + updated;
    }

    /**
     * @return number of usages ignored during processing.
     */
    public int getIgnored() {
      return ignored;
    }

    public void incrementIgnored() {
      ignored++;
    }

    /**
     * @return number of usages that were responsible for creating new nub usages.
     */
    public int getCreated() {
      return created;
    }

    public void incrementCreated() {
      created++;
      createdTotal++;
      if (createdTotal % 1000 == 0) {
        LOG.info("{} total nub usages created", createdTotal);
      }
    }

    /**
     * @return number of new nub usages with new identifiers that did not exist in the previous nub
     */
    public int getNewIds() {
      return newIds;
    }

    public void newId(int id, String scientificName) {
      newIds++;
      try {
        String[] row = {String.valueOf(id), scientificName};
        newIdWriter.write(row);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /**
     * @return number of usages that were already in the nub and updated the nub.
     */
    public int getUpdated() {
      return updated;
    }

    public void incrementUpdated() {
      updated++;
    }
  }

  public DatasetReport report(UUID key) {
    if (!reports.containsKey(key)) {
      reports.put(key, new DatasetReport());
    }
    return reports.get(key);
  }

  public void close() throws IOException {
    newIdWriter.close();
  }

  public void writeReport(Writer writer) throws IOException {
    writer.write("NUB BUILDING REPORT");

    for (Map.Entry<UUID, DatasetReport> entry : reports.entrySet()) {
      DatasetReport r = entry.getValue();

      writer.write("\n\nDataset ");
      writer.write(entry.getKey().toString());
      writer.write("\n  Total processed:" + r.getTotal());
      writer.write("\n  Nub usages created:" + r.getCreated());
      writer.write("\n  Nub usages with new ids:" + r.getNewIds());
      writer.write("\n  Nub usages updated:" + r.getUpdated());
      writer.write("\n  Usages ignored:" + r.getIgnored());
    }
  }
}
