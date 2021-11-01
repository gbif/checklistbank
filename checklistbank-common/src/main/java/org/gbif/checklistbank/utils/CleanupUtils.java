package org.gbif.checklistbank.utils;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanupUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupUtils.class);

    public static void registerCleanupHook(final File f) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                if (f.exists()) {
                    LOG.debug("Deleting file {}", f.getAbsolutePath());
                    FileUtils.deleteQuietly(f);
                }
            }
        });
    }

}
