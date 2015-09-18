package org.gbif.checklistbank.nub;

/**
 * Production backbone assertions that have to pass before we can replace the backbone with a newer version
 */
public class NubAssertions {

    private final NubDb db;

    public NubAssertions(NubDb db) {
        this.db = db;
    }

    public boolean verify() {
        // TODO: num accepted in expected range

        // TODO: num accepted per kingdom in expected range

        // TODO: num accepted in Asteraceae, Mammalia, Aves ???

        // TODO: num accepted families, genera

        // TODO: Oenanthes

        // TODO: Vertebrata

        // TODO: Jungermanniopsida

        return true;
    }
}
