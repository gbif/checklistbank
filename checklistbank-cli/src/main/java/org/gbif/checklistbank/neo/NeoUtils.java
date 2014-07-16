package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.vocabulary.Rank;

import java.io.File;
import java.util.UUID;

/**
 *
 */
public class NeoUtils {

    public static void setNumByRank(NameUsageMetrics u, Rank rank, int count) {
        if (rank == Rank.PHYLUM) {
            u.setNumPhylum(count);
        }
        if (rank == Rank.CLASS) {
            u.setNumClass(count);
        }
        if (rank == Rank.ORDER) {
            u.setNumOrder(count);
        }
        if (rank == Rank.FAMILY) {
            u.setNumFamily(count);
        }
        if (rank == Rank.GENUS) {
            u.setNumGenus(count);
        }
        if (rank == Rank.SUBGENUS) {
            u.setNumSubgenus(count);
        }
        if (rank == Rank.SPECIES) {
            u.setNumSpecies(count);
        }
    }
}
