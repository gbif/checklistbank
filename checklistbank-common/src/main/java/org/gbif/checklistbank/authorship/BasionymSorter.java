package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

/**
 * A utility to sort a list of parsed names into sets sharing the same basionym judging only the authorship not epithets.
 * A name without any authorship at all will be ignored and not returned in any group.
 */
public class BasionymSorter {
    private AuthorComparator authorComp;

    public BasionymSorter() {
        this.authorComp = AuthorComparator.createWithAuthormap();
    }

    public BasionymSorter(AuthorComparator authorComp) {
        this.authorComp = authorComp;
    }

    public Collection<BasionymGroup<ParsedName>> groupBasionyms(Iterable<ParsedName> names) {
        return groupBasionyms(names, Functions.<ParsedName>identity());
    }

    private <T> BasionymGroup<T> findExistingGroup(T p, List<BasionymGroup<T>> groups, Function<T, ParsedName> func) {
        ParsedName pn = func.apply(p);
        for (BasionymGroup<T> g : groups) {
            ParsedName representative = func.apply(g.getRecombinations().get(0));
            if (authorComp.equals(pn.getBracketAuthorship(), pn.getBracketYear(),  representative.getBracketAuthorship(), representative.getBracketYear())) {
                return g;
            }
        }
        return null;
    }

    private <T> T findBasionym(String authorship, String year, List<T> originals, Function<T, ParsedName> func) {
        for (T obj : originals) {
            ParsedName b = func.apply(obj);
            if (authorComp.equals(authorship, year,  b.getAuthorship(), b.getYear())) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Grouping that allows to use any custom class as long as there is a function that returns a ParsedName instance.
     */
    public <T> Collection<BasionymGroup<T>> groupBasionyms(Iterable<T> names, Function<T, ParsedName> func) {
        List<BasionymGroup<T>> groups = Lists.newArrayList();
        // first split names into recombinations and original names not having a basionym authorship
        // note that we drop any name without authorship here!
        List<T> recombinations = Lists.newArrayList();
        List<T> originals = Lists.newArrayList();
        for (T obj : names) {
            ParsedName p = func.apply(obj);
            if (p.getBracketAuthorship() != null || p.getBracketYear() != null) {
                recombinations.add(obj);
            } else if (p.getAuthorship() != null || p.getYear() != null) {
                originals.add(obj);
            }
        }
        // now group the recombinations
        for (T recomb : recombinations) {
            BasionymGroup<T> group = findExistingGroup(recomb, groups, func);
            // create new group if needed
            if (group == null) {
                ParsedName pn = func.apply(recomb);
                group = new BasionymGroup<T>();
                group.setName(pn.getTerminalEpithet(), pn.getBracketAuthorship(), pn.getBracketYear());
                groups.add(group);
            }
            group.getRecombinations().add(recomb);
        }
        // finally try to find the basionym for each group in the list of original names
        for (BasionymGroup<T> group : groups) {
            group.setBasionym(findBasionym(group.getAuthorship(), group.getYear(), originals, func));
        }
        return groups;
    }
}
