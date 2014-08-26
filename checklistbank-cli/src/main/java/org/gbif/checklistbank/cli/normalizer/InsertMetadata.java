package org.gbif.checklistbank.cli.normalizer;

import org.gbif.dwc.terms.Term;

import java.util.Map;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Splitter;

/**
 * Settings uses during the insert of the dwc archive into neo.
 */
public class InsertMetadata {
  private boolean coreIdUsed;
  private boolean denormedClassificationMapped;
  private boolean originalNameMapped;
  private boolean acceptedNameMapped;
  private Map<Term, Splitter> multiValueDelimiters = Maps.newHashMap();

  /**
   * @return true if the coreID of the core records is used instead of a column mapped to the taxonID term.
   */
  public boolean isCoreIdUsed() {
    return coreIdUsed;
  }

  public void setCoreIdUsed(boolean coreIdUsed) {
    this.coreIdUsed = coreIdUsed;
  }

  public boolean isDenormedClassificationMapped() {
    return denormedClassificationMapped;
  }

  public void setDenormedClassificationMapped(boolean denormedClassificationMapped) {
    this.denormedClassificationMapped = denormedClassificationMapped;
  }

  public boolean isOriginalNameMapped() {
    return originalNameMapped;
  }

  public void setOriginalNameMapped(boolean originalNameMapped) {
    this.originalNameMapped = originalNameMapped;
  }

  public boolean isAcceptedNameMapped() {
    return acceptedNameMapped;
  }

  public void setAcceptedNameMapped(boolean acceptedNameMapped) {
    this.acceptedNameMapped = acceptedNameMapped;
  }

  public Map<Term, Splitter> getMultiValueDelimiters() {
    return multiValueDelimiters;
  }
}
