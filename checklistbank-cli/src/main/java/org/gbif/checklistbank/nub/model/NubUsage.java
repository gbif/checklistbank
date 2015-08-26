package org.gbif.checklistbank.nub.model;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.neo4j.graphdb.Node;

public class NubUsage {

    public int usageKey;
    public String publishedIn;
    public Rank rank;
    public UUID datasetKey;
    public Origin origin;
    public ParsedName parsedName;
    public TaxonomicStatus status;
    public Set<NomenclaturalStatus> nomStatus = Sets.newHashSet();
    public Node node;
    public Kingdom kingdom;
    public List<Integer> sourceIds = Lists.newArrayList();
    public Set<String> authors = Sets.newHashSet();
    public Set<NameUsageIssue> issues = EnumSet.noneOf(NameUsageIssue.class);

    public NubUsage() {
    }

    public NubUsage(SrcUsage u) {
        publishedIn = u.publishedIn;
        parsedName = u.parsedName;
        rank = u.rank;
        status = u.status;
        addNomStatus(u.nomStatus);
    }

    public void addNomStatus(NomenclaturalStatus[] nomStatus) {
        if (nomStatus != null) {
            for (NomenclaturalStatus ns : nomStatus) {
                this.nomStatus.add(ns);
            }
        }
    }

}
