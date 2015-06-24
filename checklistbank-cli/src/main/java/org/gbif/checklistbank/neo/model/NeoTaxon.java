package org.gbif.checklistbank.neo.model;

import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;

import java.util.Set;

import org.neo4j.graphdb.Node;

public class NeoTaxon {
    public Node node;
    public String taxonID;
    public Rank rank;
    public Origin origin;
    public String scientificName;
    public String canonicalName;
    public Set<NameUsageIssue> issues;
    public String remarks;
}
