package org.gbif.checklistbank.nub;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParentStack {
  private static final Logger LOG = LoggerFactory.getLogger(ParentStack.class);
  private Map<Integer, NubUsage> nubMap = Maps.newHashMap();
  private LinkedList<SrcUsage> parents = Lists.newLinkedList();
  private NubUsage currParent;
  private Kingdom currKingdom;
  private Integer doubtfulKey = null;
  private NubUsage defaultParent;

  public ParentStack(NubUsage defaultParent) {
    setDefaultParent(defaultParent);
  }

  void setDefaultParent(NubUsage defaultParent) {
    this.defaultParent = defaultParent;
    currKingdom = Preconditions.checkNotNull(defaultParent.kingdom, "default parent needs to have a kingdom");
  }

  /**
   * Returns the current lowest nub parent node. If no parent was set the unknown kingdom is returned, never null.
   */
  public NubUsage nubParent() {
    return currParent == null ? defaultParent : currParent;
  }

  /**
   * @return the nub kingdom the current classification is rooted in
   */
  public Kingdom nubKingdom() {
    return currKingdom;
  }

  public int size() {
    return parents.size();
  }

  /**
   * Puts a new source usage onto the stack.
   * If the parentKey of the new usage matches the currently last source usage on the stack it is simply added.
   * Otherwise the current stack is reduced as long until the parentKey matches the key of the last usage on the stack
   * @param src
   */
  public void add(SrcUsage src) {
    if (src.parentKey == null) {
      // this is a new root source usage, clear stack
      clear();

    } else {
      while (!parents.isEmpty()) {
        if (parents.getLast().key.equals(src.parentKey)) {
          // the last src usage on the parent stack represents the current parentKey, we are in good state!
          break;
        } else {
          // remove last parent until we find the real one
          SrcUsage p = parents.removeLast();
          nubMap.remove(p.key);
          // reset doubtful marker if the taxon gets removed from the stack
          if (doubtfulKey != null && doubtfulKey.equals(p.key)) {
            doubtfulKey = null;
          }
        }
      }
      if (parents.isEmpty()) {
        throw new IllegalStateException("Source parent node " + src.parentKey + " not found for " + src.scientificName);
      }
      // set current nub parent
      currParent = null;
      Iterator<SrcUsage> iter = parents.descendingIterator();
      while (iter.hasNext()) {
        SrcUsage u = iter.next();
        if (u.key != null && nubMap.containsKey(u.key)) {
          currParent = nubMap.get(u.key);
          break;
        }
      }
    }
    parents.add(src);
  }

  /**
   * @return true if the current sources immediate parent is part of the nub
   */
  public boolean parentInNub() {
    SrcUsage curr = parents.getLast();
    if (curr != null) {
      return nubMap.containsKey(curr.parentKey);
    }
    return false;
  }

  public boolean parentsContain(List<String> names) {
    for (SrcUsage u : parents) {
      for (String name : names) {
        if (StringUtils.trimToEmpty(u.scientificName).equalsIgnoreCase(name) ||
                StringUtils.trimToEmpty(u.parsedName.canonicalName()).equalsIgnoreCase(name)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Sets the doubtful flag for the current usage and all its descendants.
   */
  public void markSubtreeAsDoubtful() {
    if (!parents.isEmpty() && doubtfulKey == null) {
      doubtfulKey = parents.getLast().key;
    }
  }

  public boolean isDoubtful() {
    return doubtfulKey != null;
  }

  public LinkedList<SrcUsage> getParents() {
    return parents;
  }

  /**
   * Assigns the matching nub node to the currently last source usage on the stack, replacing whatever was there.
   */
  public void put(NubUsage nub) {
    nubMap.put(parents.getLast().key, nub);
    currKingdom = nub.kingdom;
  }

  public void clear() {
    nubMap.clear();
    parents.clear();
    currParent = null;
    currKingdom = defaultParent.kingdom;
    doubtfulKey = null;
  }
}
