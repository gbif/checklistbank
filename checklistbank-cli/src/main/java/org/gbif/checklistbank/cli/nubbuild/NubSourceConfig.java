package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.model.RankedName;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NubSourceConfig {

    public NubSourceConfig() {
    }

    public NubSourceConfig(UUID key) {
        this.key = key;
    }

    public NubSourceConfig(UUID key, Rank rank) {
        this.key = key;
        this.rank = rank;
    }

    public NubSourceConfig(UUID key, Rank rank, boolean homonyms, boolean synonyms) {
        this.key = key;
        this.rank = rank;
        this.homonyms = homonyms;
        this.synonyms = synonyms;
    }

    /**
     * Dataset, organisation or installation key.
     * If organisation or installations are used as sources ALL their registered checklists will be used.
     */
    @NotNull
    public UUID key;

    /**
     * Maximum rank to be considered for addition to the backbone.
     * Defaults to family
     */
    public Rank rank = Rank.FAMILY;

    /**
     * If true suprageneric homonyms are allowed during nub builds from this dataset.
     */
    public boolean homonyms = false;

    /**
     * If true marks a source from which synonyms should be ignored during nub builds.
     * E.g. Plazi organization due to http://dev.gbif.org/issues/browse/POR-3151
     */
    public boolean synonyms = true;

    /**
     * If true marks a source from which OTU names can be taken.
     * Default is not to use OTUs
     */
    public boolean OTU = false;

    /**
     * List of taxon names to be excluded from that source,
     * so these groups do not make it to the backbone. This applies to all its descendants - different to how the blacklist behaves.
     */
    public List<RankedName> exclude = new ArrayList<>();

}
