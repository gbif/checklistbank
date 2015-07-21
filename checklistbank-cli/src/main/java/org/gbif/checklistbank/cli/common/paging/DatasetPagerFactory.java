package org.gbif.checklistbank.cli.common.paging;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that returns the appropriate dataset iterator for any given uuid key
 */
public class DatasetPagerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetPagerFactory.class);

    /**
     * @param key a valid dataset, organization or installation key. If null all datasets will be iterated over
     * @throws IllegalArgumentException if given key is not existing
     */
    public static Iterable<Dataset> datasets(@Nullable UUID key, @Nullable DatasetType type, DatasetService ds, OrganizationService os, InstallationService is) {
        if (key == null) {
            LOG.info("Iterate over all {} datasets", type == null ? "" : type);
            return new DatasetPager(ds, type);

        } else if (isDataset(key, ds)) {
            LOG.info("Iterate over dataset {}", key);
            return ImmutableList.of(ds.get(key));

        } else if (isOrganization(key, os)) {
            LOG.info("Iterate over all {} datasets published by {}", type == null ? "" : type, key);
            return new OrgPublishingPager(os, key, type);

        } else if (isInstallation(key, is)) {
            LOG.info("Iterate over all {} datasets hosted by installation {}", type == null ? "" : type, key);
            return new InstallationPager(is, key, type);
        }
        throw new IllegalArgumentException("Given key is no valid GBIF registry key: " + key);
    }

    private static boolean isDataset(UUID key, DatasetService ds) {
        return ds.get(key) != null;
    }

    private static boolean isOrganization(UUID key, OrganizationService os) {
        return os.get(key) != null;
    }

    private static boolean isInstallation(UUID key, InstallationService is) {
        return is.get(key) != null;
    }
}
