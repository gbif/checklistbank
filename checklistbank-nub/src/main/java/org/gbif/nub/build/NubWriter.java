package org.gbif.nub.build;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.file.TabWriter;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tools that generate all files for a new nub build.
 * Should be able to share this class at some point with importing routines when we get there.
 * The files generated are ready to be quickly inserted into teh clb database.
 */
public class NubWriter {
  private static final Logger LOG = LoggerFactory.getLogger(NubWriter.class);
  private final File dir;
  private final DatasetImportService importService;
  private TabWriter nubRelWriter;
  private UUID currentDatasetKey;
  private Set<UUID> processedDatasets = Sets.newHashSet();

  @Inject
  public NubWriter(DatasetImportService importService, File reportDir) {
    this.importService = importService;
    this.dir = reportDir;
  }

  /**
   * Starts a new set of source dataset files and closes the last one if it was still open.
   */
  public void startDataset(UUID datasetKey) {
    currentDatasetKey = datasetKey;
    closeQuietly(nubRelWriter);
    nubRelWriter = open(nubRelFile(datasetKey));
    processedDatasets.add(datasetKey);
  }

  private File nubRelFile(UUID datasetKey) {
    return new File(dir, "nubkeys-"+datasetKey.toString()+".tab");
  }

  public void close() {
    closeQuietly(nubRelWriter);
  }

  public void closeQuietly(TabWriter w) {
    if (w != null) {
      try {
        w.close();
      } catch (IOException e) {
        LOG.warn("Cannot close tab writer", e);
      }
    }
  }

  public void mapSource(int srcUsageKey, int nubUsageKey) {
    Preconditions.checkNotNull("no dataset yet started", nubRelWriter);
    try {
      String[] row = {String.valueOf(srcUsageKey), String.valueOf(nubUsageKey)};
      nubRelWriter.write(row);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write nub key map", e);
    }
  }

  /**
   * Deletes any previous nub from checklistbank and inserts a new one based on the nub cache.
   */
  public void insertNubData(ChecklistCache nubCache) {
    LOG.info("Write nub data to files in directory {}", dir);

    // delete old nub
    importService.deleteDataset(Constants.NUB_DATASET_KEY);

    // insert new nub usage
    importService.insertUsages(Constants.NUB_DATASET_KEY, nubCache.iterate());
  }

  private TabWriter open(File f) {
    try {
      return new TabWriter(new FileOutputStream(f));
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("Cannot open tab writer for file " + f.getAbsolutePath(), e);
    }
  }

  /**
   * Uses the mapped source files to update all nub relations for those datasets.
   */
  public void updateRelNub() {
    for (UUID datasetKey : processedDatasets) {
      try {
        Map<Integer, Integer> relations = readRelFile(nubRelFile(datasetKey));
        LOG.info("Updating dataset {} with {} nub relations", datasetKey, relations.size());
        importService.insertNubRelations(datasetKey, relations);
      } catch (FileNotFoundException e) {
        LOG.error("Cannot read nub relation file for dataset {}. Warning, dataset relations are unchanged!", datasetKey);
      }
    }
  }

  private Map<Integer, Integer> readRelFile(File f) throws FileNotFoundException {
    Map<Integer, Integer> rels = Maps.newHashMap();
    LineIterator lines = FileUtils.getLineIterator(FileUtils.getInputStream(f));
    while (lines.hasNext()) {
      String[] parts = FileUtils.TAB_DELIMITED.split(lines.nextLine().trim());
      rels.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    return rels;
  }
}
