package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;

import java.util.UUID;

/**
 * Very simple POJO representing a backbone source dataset.
 */
public class NubSource {
  public UUID key;
  public String name;
  public int priority = 0;
  public Rank ignoreRanksAbove = Rank.FAMILY;
  public boolean ignoreCore = false;
  public boolean ignoreMedia = false;
  public boolean ignoreDescription = false;
  public boolean ignoreVernacular = false;
}
