package org.gbif.checklistbank.authorship;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;


public class BasionymGroup<T> {
    private static final Joiner joiner = Joiner.on("; ").skipNulls();
    private T basionym;
    private List<T> recombinations = Lists.newArrayList();

    public BasionymGroup() {
    }

    public BasionymGroup(T basionym, List<T> recombinations) {
        this.basionym = basionym;
        this.recombinations = recombinations;
    }

    public T getBasionym() {
        return basionym;
    }

    public void setBasionym(T basionym) {
        this.basionym = basionym;
    }

    public List<T> getRecombinations() {
        return recombinations;
    }

    public boolean hasBasionym() {
        return basionym != null;
    }

    public boolean hasRecombinations() {
        return !recombinations.isEmpty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(basionym, recombinations);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BasionymGroup other = (BasionymGroup) obj;
        return Objects.equals(this.basionym, other.basionym)
                && Objects.equals(this.recombinations, other.recombinations);
    }

    @Override
    public String toString() {
        return "BasionymGroup{" +
                basionym +
                ": " + joiner.join(recombinations) + '}';
    }
}
