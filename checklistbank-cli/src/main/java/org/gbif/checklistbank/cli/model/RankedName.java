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
package org.gbif.checklistbank.cli.model;

import org.gbif.api.vocabulary.Rank;

import java.util.Objects;

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
