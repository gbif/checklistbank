package org.gbif.checklistbank.cli.common;

import org.gbif.api.vocabulary.Rank;

import org.neo4j.graphdb.Node;

public class RankedName {
  public String name;
  public Rank rank;
  public Node node;

  public RankedName() {
  }

  public RankedName(String name, Rank rank) {
    this.name = name;
    this.rank = rank;
  }

  @Override
  public String toString() {
    return name + '[' + rank + ']';
  }
}
