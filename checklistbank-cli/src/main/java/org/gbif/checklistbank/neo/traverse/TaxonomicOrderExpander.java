package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

/**
 * depth first, rank then scientific name order based branching.
 */
public class TaxonomicOrderExpander implements PathExpander {
  /**
   * Expander following the parent_of relations in taxonomic order
   */
  public final static TaxonomicOrderExpander TREE_EXPANDER = new TaxonomicOrderExpander();

  /**
   * Expander following the parent_of and synonym_of relations in taxonomic order
   */
  public final static TaxonomicOrderExpander TREE_WITH_SYNONYMS_EXPANDER = new TaxonomicOrderExpander(RelType.SYNONYM_OF);

  /**
   * Expander following the parent_of, synonym_of and proparte_synonym_of relations in taxonomic order
   */
  public final static TaxonomicOrderExpander TREE_WITH_PPSYNONYMS_EXPANDER = new TaxonomicOrderExpander(RelType.SYNONYM_OF, RelType.PROPARTE_SYNONYM_OF);

  private final Set<RelType> synRels;

  private static final Ordering<Relationship> CHILDREN_ORDER = Ordering.from(new TaxonomicOrder()).onResultOf(
      new Function<Relationship, Node>() {
        @Nullable
        @Override
        public Node apply(Relationship rel) {
          return rel.getEndNode();

        }
      }
  );

  private static final Ordering<Relationship> SYNONYM_ORDER = Ordering.natural().reverse().onResultOf(
      new Function<Relationship, Boolean>() {
        @Nullable
        @Override
        public Boolean apply(Relationship rel) {
          return rel.getStartNode().hasLabel(Labels.BASIONYM);
        }
      }
  ).compound(
      Ordering.from(new TaxonomicOrder()).onResultOf(
          new Function<Relationship, Node>() {
            @Nullable
            @Override
            public Node apply(Relationship rel) {
              return rel.getStartNode();

            }
          }
      )
  );

  private TaxonomicOrderExpander(RelType ... synonymRelations) {
    if (synonymRelations == null) {
      this.synRels = ImmutableSet.of();
    } else {
      this.synRels = ImmutableSet.copyOf(synonymRelations);
    }
  }

  @Override
  public Iterable<Relationship> expand(Path path, BranchState state) {
    List<Relationship> children = CHILDREN_ORDER.sortedCopy(path.endNode().getRelationships(RelType.PARENT_OF, Direction.OUTGOING));
    if (synRels.isEmpty()) {
      return children;
    } else {
      List<Iterable<Relationship>> synResults = Lists.newArrayList();
      for (RelType rt : synRels) {
        synResults.add(path.endNode().getRelationships(rt, Direction.INCOMING));
      }
      return Iterables.concat(
          SYNONYM_ORDER.sortedCopy(Iterables.concat(synResults)),
          children
      );
    }
  }

  @Override
  public PathExpander reverse() {
    throw new UnsupportedOperationException();
  }

}
