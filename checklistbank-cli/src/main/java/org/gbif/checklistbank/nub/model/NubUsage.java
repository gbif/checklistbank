package org.gbif.checklistbank.nub.model;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.neo4j.graphdb.Node;

public class NubUsage {

  public NubUsage() {
  }

  public NubUsage(SrcUsage u) {
    publishedIn=u.publishedIn;
    parsedName=u.parsedName;
    rank=u.rank;
    status=u.status;
    addNomStatus(u.nomStatus);
  }

  // properties shared with NameUsage
  public int nubKey;
  public String publishedIn;
  public Rank rank;
  public UUID datasetKey;
  public Origin origin;
  // properties shared with SrcUsage
  public ParsedName parsedName;
  public TaxonomicStatus status;
  public Set<NomenclaturalStatus> nomStatus = Sets.newHashSet();
  // extra properties
  public Node node;
  public Kingdom kingdom_; // underscore as property name clashes with NameUsage.kingdom (String) if stored in the same neo node
  public List<Integer> sourceIds = Lists.newArrayList();
  public Set<String> authors = Sets.newHashSet();

  public void addNomStatus(NomenclaturalStatus[] nomStatus) {
    if (nomStatus != null) {
      for (NomenclaturalStatus ns : nomStatus) {
        this.nomStatus.add(ns);
      }
    }
  }

  public int getKey() {
    return (int) node.getId();
  }
}
