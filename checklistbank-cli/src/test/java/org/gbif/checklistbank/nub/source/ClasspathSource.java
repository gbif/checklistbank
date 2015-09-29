package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.utils.file.InputStreamUtils;

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

/**
 * UsageSource implementation that works on classpath files for testing nub builds.
 * This class is just a test helper class, see NubBuilderTest for its use.
 * For every dataset source there needs to be simple flat usage tab file under resources/nub-sources
 * with the following columns:
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 * <li>usageKey</li>
 * <li>parentKey</li>
 * <li>basionymKey</li>
 * <li>rank (enum)</li>
 * <li>isSynonym (f/t)</li>
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * </ul>
 */
public class ClasspathSource extends NubSource {
    private final ClasspathUsageIteratorNeo iter;

    public ClasspathSource(Integer id) {
        key = IdxToKey(id);
        name = "Dataset " + id;
        priority = id;
        try {
            iter = new ClasspathUsageIteratorNeo(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        iter.init();
    }

    @Override
    public CloseableIterable<SrcUsage> usages() {
        return iter;
    }

    public class ClasspathUsageIteratorNeo extends UsageIteratorNeo {

        public ClasspathUsageIteratorNeo(ClasspathSource source) throws Exception {
            super(128, source.key, source.name);
        }

        @Override
        void initNeo(NeoUsageWriter writer) throws Exception {
            try (InputStream is = openTxtStream(keyToIdx(datasetKey))) {
                IOUtils.copy(is, writer);
            }
        }
    }

    private static Integer keyToIdx(UUID key) {
        return Integer.valueOf(key.toString().substring(34));
    }

    private static UUID IdxToKey(Integer id) {
        return UUID.fromString(String.format("d7dddbf4-2cf0-4f39-9b2a-99b0e2c3a%02d", id));
    }

    private static InputStream openTxtStream(Integer id) {
        String file = "nub-sources/dataset" + id + ".txt";
        InputStreamUtils isu = new InputStreamUtils();
        return isu.classpathStream(file);
    }
}
