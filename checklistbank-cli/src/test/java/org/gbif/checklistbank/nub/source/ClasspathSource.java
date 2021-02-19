package org.gbif.checklistbank.nub.source;

import org.apache.commons.io.IOUtils;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.nub.NubBuilderIT;
import org.gbif.utils.file.InputStreamUtils;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * NubSource implementation that works on classpath files for testing nub builds and uses an in memory DAO.
 * This class is just a test helper class, see NubBuilderTest for its use.
 * For every dataset source there needs to be simple flat usage tab file under resources/nub-sources
 * with the following columns:
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 * <li>usageKey</li>
 * <li>parentKey</li>
 * <li>basionymKey</li>
 * <li>rank (enum)</li>
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * <li>namePublishedIn</li>
 * </ul>
 */
public class ClasspathSource extends NubSource {
  public final int id;

  public ClasspathSource(int id, NeoConfiguration neo) {
    this(id, false, neo);
  }

  public ClasspathSource(int id, boolean tmp, NeoConfiguration neo) {
    this(id, null, tmp, neo);
  }

  public ClasspathSource(int id, @Nullable List<RankedName> exclusion, boolean tmp, NeoConfiguration neo) {
    super(UUID.randomUUID(), "Dataset " + id, exclusion, tmp, neo);
    this.id = id;
  }

  @Override
  public void initNeo(NeoUsageWriter writer) throws Exception {
    String file = "nub-sources/dataset" + id + ".txt";
    InputStreamUtils isu = new InputStreamUtils();
    try (InputStream is = isu.classpathStream(file)) {
      IOUtils.copy(is, writer);
    }
  }
}
