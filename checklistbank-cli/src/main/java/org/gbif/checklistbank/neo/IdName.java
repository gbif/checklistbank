package org.gbif.checklistbank.neo;

/**
 * Very simple POJO tying a name to a long identifier.
 */
public class IdName {
  public int id;
  public String name;

  @Override
  public String toString() {
    return "IdName{" +
           "id=" + id +
           ", name='" + name + '\'' +
           '}';
  }
}
