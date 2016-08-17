package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility to sort a list of parsed names into sets sharing the same basionym judging only the authorship not epithets.
 * A name without any authorship at all will be ignored and not returned in any group.
 */
public class BasionymSorter {
  private static final Logger LOG = LoggerFactory.getLogger(BasionymSorter.class);
  private AuthorComparator authorComp;

  public BasionymSorter() {
    this.authorComp = AuthorComparator.createWithAuthormap();
  }

  public BasionymSorter(AuthorComparator authorComp) {
    this.authorComp = authorComp;
  }

  public static class MultipleBasionymException extends Exception {

  }

  public Collection<BasionymGroup<ParsedName>> groupBasionyms(Iterable<ParsedName> names) {
    return groupBasionyms(names, Functions.<ParsedName>identity());
  }

  private <T> BasionymGroup<T> findExistingGroup(T p, List<BasionymGroup<T>> groups, Function<T, ParsedName> func) {
    ParsedName pn = func.apply(p);
    for (BasionymGroup<T> g : groups) {
      ParsedName representative = func.apply(g.getRecombinations().get(0));
      if (authorComp.compareStrict(pn.getBracketAuthorship(), pn.getBracketYear(), representative.getBracketAuthorship(), representative.getBracketYear())) {
        return g;
      }
    }
    return null;
  }

  private <T> T findBasionym(String authorship, String year, List<T> originals, Function<T, ParsedName> func) throws MultipleBasionymException {
    List<T> basionyms = Lists.newArrayList();
    for (T obj : originals) {
      ParsedName b = func.apply(obj);
      if (authorComp.compareStrict(authorship, year, b.getAuthorship(), b.getYear())) {
        basionyms.add(obj);
      }
    }
    if (basionyms.isEmpty()) {
      // try again without year in case we didnt find any but make sure we only match once!
      if (authorship != null) {
        for (T obj : originals) {
          ParsedName b = func.apply(obj);
          if (authorComp.compareStrict(authorship, null, b.getAuthorship(), null)) {
            basionyms.add(obj);
          }
        }
      }
    }

    // we have more than one match, dont use it!
    if (basionyms.size() == 1) {
      return basionyms.get(0);
    } else if (basionyms.isEmpty()) {
      return null;
    }

    throw new MultipleBasionymException();
  }

  /**
   * Grouping that allows to use any custom class as long as there is a function that returns a ParsedName instance.
   * The list of groups returned only contains groups with no or one known basionym. Any uncertain cases like groups with multiple basionyms are excluded!
   */
  public <T> Collection<BasionymGroup<T>> groupBasionyms(Iterable<T> names, Function<T, ParsedName> func) {
    List<BasionymGroup<T>> groups = Lists.newArrayList();
    // first split names into recombinations and original names not having a basionym authorship
    // note that we drop any name without authorship here!
    List<T> recombinations = Lists.newArrayList();
    List<T> originals = Lists.newArrayList();
    for (T obj : names) {
      ParsedName p = func.apply(obj);
      if (p != null) {
        if (p.isRecombination()) {
          recombinations.add(obj);
        } else if (p.getAuthorship() != null || p.getYear() != null) {
          originals.add(obj);
        }
      } else {
        LOG.warn("No parsed name returned for name object {}", obj);
      }
    }

    // now group the recombinations
    for (T recomb : recombinations) {
      BasionymGroup<T> group = findExistingGroup(recomb, groups, func);
      // create new group if needed
      if (group == null) {
        ParsedName pn = func.apply(recomb);
        if (pn != null) {
          group = new BasionymGroup<T>();
          group.setName(pn.getTerminalEpithet(), pn.getBracketAuthorship(), pn.getBracketYear());
          groups.add(group);
          group.getRecombinations().add(recomb);
        } else {
          LOG.warn("No parsed name returned for name recombination {}", recomb);
        }
      } else {
        group.getRecombinations().add(recomb);
      }
    }
    // finally try to find the basionym for each group in the list of original names
    Iterator<BasionymGroup<T>> iter = groups.iterator();
    while (iter.hasNext()) {
      BasionymGroup<T> group = iter.next();
      try {
        group.setBasionym(findBasionym(group.getAuthorship(), group.getYear(), originals, func));
      } catch (MultipleBasionymException e) {
        LOG.info("Ignore group with multiple basionyms found for {} {} {} in {} original names", group.getEpithet(), group.getAuthorship(), group.getYear(), originals.size());
        iter.remove();
      }
    }
    return groups;
  }
}
