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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.carrotsearch.hppc.IntArrayList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.neo4j.graphdb.Node;

public class NubUsage {

    public int usageKey;
    public String publishedIn;
    public String scientificNameID;
    public Rank rank;
    public UUID datasetKey;
    public Origin origin;
    public ParsedName parsedName;
    public TaxonomicStatus status;
    public Set<NomenclaturalStatus> nomStatus = Sets.newHashSet();
    public Node node;
    public Kingdom kingdom;
    public IntArrayList sourceIds = new IntArrayList();
    //public Set<String> authors = Sets.newHashSet();
    public Set<NameUsageIssue> issues = EnumSet.noneOf(NameUsageIssue.class);
    public List<String> remarks = Lists.newArrayList();

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

    public void addRemark(String remark) {
        remarks.add(remark);
    }

    @Override
    public String toString() {
        if (rank != null && parsedName != null && node != null) {
            return rank + " " + parsedName.getScientificName() + " [" + node.getId() + "]";
        }
        return super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NubUsage nubUsage = (NubUsage) o;
        return Objects.equals(usageKey, nubUsage.usageKey) &&
                Objects.equals(publishedIn, nubUsage.publishedIn) &&
                Objects.equals(scientificNameID, nubUsage.scientificNameID) &&
                Objects.equals(rank, nubUsage.rank) &&
                Objects.equals(datasetKey, nubUsage.datasetKey) &&
                Objects.equals(origin, nubUsage.origin) &&
                Objects.equals(parsedName, nubUsage.parsedName) &&
                Objects.equals(status, nubUsage.status) &&
                Objects.equals(nomStatus, nubUsage.nomStatus) &&
                Objects.equals(node, nubUsage.node) &&
                Objects.equals(kingdom, nubUsage.kingdom) &&
                Objects.equals(sourceIds, nubUsage.sourceIds) &&
                Objects.equals(issues, nubUsage.issues) &&
                Objects.equals(remarks, nubUsage.remarks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usageKey, publishedIn, scientificNameID, rank, datasetKey, origin, parsedName, status, nomStatus, node, kingdom, sourceIds, issues, remarks);
    }

    public String toStringComplete() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("usageKey", usageKey)
                .add("publishedIn", publishedIn)
                .add("scientificNameID", scientificNameID)
                .add("rank", rank)
                .add("origin", origin)
                .add("parsedName", parsedName)
                .add("status", status)
                .add("nomStatus", nomStatus)
                .add("node", node)
                .add("kingdom", kingdom)
                .add("sourceIds", sourceIds)
                .add("issues", issues)
                .add("remarks", remarks)
                .add("datasetKey", datasetKey)
                .toString();
    }
}
