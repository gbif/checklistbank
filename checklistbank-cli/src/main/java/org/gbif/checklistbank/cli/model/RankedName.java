package org.gbif.checklistbank.cli.model;

import org.gbif.api.vocabulary.Rank;

import org.neo4j.graphdb.Node;

import java.util.Objects;

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

  public int getId() {
    return (int) node.getId();
  }

  @Override
  public String toString() {
    return name + '[' + rank + ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RankedName)) return false;
    RankedName that = (RankedName) o;
    return Objects.equals(name, that.name) && rank == that.rank && Objects.equals(node, that.node);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, rank, node);
  }
}
