/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.model.RankedName;

import java.util.*;

import javax.validation.constraints.NotNull;

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
     * If true unranked names are processed during nub builds from this dataset.
     */
    public boolean unranked = false;

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
     * The taxonomic scope of the source dataset.
     * Name to use as the parent for all incertae sedis names when they snap to the backbone.
     */
    public RankedName scope;

    /**
     * Custom mapping of name types to be applied to this source only.
     * Allows to correct badly detected informal etc names.
     */
    public Map<NameType, NameType> nameTypeMapping = new HashMap<>();

    /**
     * List of taxon names to be excluded from that source,
     * so these groups do not make it to the backbone. This applies to all its descendants - different to how the blacklist behaves.
     */
    public List<RankedName> exclude = new ArrayList<>();

}
