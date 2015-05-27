package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.utils.file.InputStreamUtils;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.google.common.io.Closeables;
import org.apache.commons.io.IOUtils;

/**
 * UsageSource implementation that works on classpath files for testing nub builds.
 * This class is just a test helper class, see NubBuilderTest for its use.
 *
 * For every dataset source there needs to be simple flat usage tab file under resources/nub-sources
 * with the following columns:
 *
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 *   <li>usageKey</li>
 *   <li>parentKey</li>
 *   <li>basionymKey</li>
 *   <li>rank (enum)</li>
 *   <li>isSynonym (f/t)</li>
 *   <li>taxonomicStatus (enum)</li>
 *   <li>nomenclaturalStatus (enum[])</li>
 *   <li>scientificName</li>
 * </ul>
 */
public class ClasspathUsageSource implements UsageSource {
  List<NubSource> sources = Lists.newArrayList();

  /**
   * Creates a classpath based source that uses no resources at all.
   */
  public static ClasspathUsageSource emptySource() {
    return new ClasspathUsageSource();
  }

  /**
   * Creates a classpath based source that uses all classpath resources under /nub-sources
   */
  public static ClasspathUsageSource allSources() {
    // figure out which files we have dynamically
    List<Integer> ids = Lists.newArrayList();
    for (int id=0; id < 100; id++) {
      InputStream is = openTxtStream(id);
      if (is != null) {
        ids.add(id);
        Closeables.closeQuietly(is);
      }
    }
    return new ClasspathUsageSource(ids);
  }

  /**
   * Creates a classpath based source that uses just the specieid classpath resources under /nub-sources
   */
  public static ClasspathUsageSource source(Integer ... datasetKeys) {
    return new ClasspathUsageSource(Lists.newArrayList(datasetKeys));
  }

  private ClasspathUsageSource() {
  }

  private ClasspathUsageSource(List<Integer> datasetKeys) {
    for (Integer id : datasetKeys) {
      NubSource src = new NubSource();
      src.key = IdxToKey(id);
      src.name = "Dataset " + id;
      src.priority = id;
      sources.add(src);
    }
  }

  public class ClasspathUsageIteratorNeo extends UsageIteratorNeo {

    public ClasspathUsageIteratorNeo(NubSource source) throws Exception {
      super(source);
    }

    @Override
    void initNeo(NeoUsageWriter writer) throws Exception {
      try (InputStream is = openTxtStream(keyToIdx(source.key))) {
        IOUtils.copy(is, writer);
      }
    }
  }

  @Override
  public Iterable<SrcUsage> iterateSource(NubSource source) {
    try {
      return new ClasspathUsageIteratorNeo(source);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<NubSource> listSources() {
    return sources;
  }

  /**
   * Sets the higher rank setting of a given nub source which defaults to family if not set explicitly.
   */
  public void setSourceRank(int sourceId, Rank rank) {
    final UUID key = IdxToKey(sourceId);
    for (NubSource src : sources) {
      if (src.key.equals(key)) {
        src.ignoreRanksAbove = rank;
        break;
      }
    }
  }

  private static Integer keyToIdx(UUID key) {
    return Integer.valueOf(key.toString().substring(35));
  }

  private static UUID IdxToKey(Integer id) {
    return UUID.fromString(String.format("d7dddbf4-2cf0-4f39-9b2a-99b0e2c3aa%s", id));
  }

  private static InputStream openTxtStream(Integer id) {
    String file = "nub-sources/dataset" + id + ".txt";
    InputStreamUtils isu = new InputStreamUtils();
    return isu.classpathStream(file);
  }
}
