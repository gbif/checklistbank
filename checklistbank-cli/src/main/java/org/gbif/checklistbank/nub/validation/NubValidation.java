package org.gbif.checklistbank.nub.validation;

/**
 *
 */
public interface NubValidation {

  /**
   * Implementing class can expect an open neo4j transaction to exist!
   * @return true if validation succeeded
   */
  public boolean validate();

}
