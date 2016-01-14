package org.gbif.checklistbank.nub;

/**
 *
 */
public interface TreeValidation {

  /**
   * Implementing class can expect an open neo4j transaction to exist!
   * @return true if validation succeeded
   */
  public boolean validate();

}
