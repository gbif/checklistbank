package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
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
        List<BasionymGroup<ParsedName>> groups = Lists.newArrayList();
        // first split names into recombinations and original names not having a basionym authorship
        // note that we drop any name without authorship here!
        List<ParsedName> recombinations = Lists.newArrayList();
        List<ParsedName> originals = Lists.newArrayList();
        for (ParsedName p : names) {
            if (p.getBracketAuthorship() != null || p.getBracketYear() != null) {
                recombinations.add(p);
            } else if (p.getAuthorship() != null || p.getYear() != null) {
                originals.add(p);
            }
        }
        // now group the recombinations
        for (ParsedName p : recombinations) {
            BasionymGroup<ParsedName> group = findExistingGroup(p, groups);
            // create new group if needed
            if (group == null) {
                group = new BasionymGroup<ParsedName>();
                groups.add(group);
            }
            group.getRecombinations().add(p);
        }
        // finally try to find the basionym for each group in the list of original names
        for (BasionymGroup<ParsedName> group : groups) {
            group.setBasionym(findBasionym(group.getRecombinations().get(0), originals));
        }
        return groups;
    }

    private BasionymGroup<ParsedName> findExistingGroup(ParsedName p, List<BasionymGroup<ParsedName>> groups) {
        for (BasionymGroup<ParsedName> g : groups) {
            ParsedName representative = g.getRecombinations().get(0);
            if (authorComp.equals(p.getBracketAuthorship(), p.getBracketYear(),  representative.getBracketAuthorship(), representative.getBracketYear())) {
                return g;
            }
        }
        return null;
    }

    private ParsedName findBasionym(ParsedName recomb, List<ParsedName> originals) {
        for (ParsedName b : originals) {
            if (authorComp.equals(recomb.getBracketAuthorship(), recomb.getBracketYear(),  b.getAuthorship(), b.getYear())) {
                return b;
            }
        }
        return null;
    }

    /**
     * Grouping that allows to use any custom class as long as there is a function that returns a ParsedName instance.
     */
    public <T> Collection<BasionymGroup<T>> groupBasionyms(Iterable<T> names, Function<T, ParsedName> func) {
        return null;
    }
}
