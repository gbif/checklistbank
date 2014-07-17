package org.gbif.checklistbank.neo.traverse;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * depth first, rank then scientific name order based branching.
 */
public class TaxonomicOrderExpander implements PathExpander {
  private static final Logger LOG = LoggerFactory.getLogger(TaxonomicOrderExpander.class);
  private boolean reverse = false;
  private final Ordering<Relationship> TAX_ORDER =
    Ordering
      .natural()
      .onResultOf(new Function<Relationship, Integer>() {
        @Nullable
        @Override
        public Integer apply(Relationship rel) {
          return (Integer) rel.getEndNode().getProperty(TaxonProperties.RANK);
        }
      })
      .compound(
        Ordering
          .natural()
          .onResultOf(new Function<Relationship, String>() {
            @Nullable
            @Override
            public String apply(Relationship rel) {
              return (String) rel.getEndNode().getProperty(TaxonProperties.CANONICAL_NAME);
            }
          })
      );

  @Override
  public Iterable<Relationship> expand(Path path, BranchState state) {
    List<Relationship> children = Lists.newArrayList(IteratorUtil.asCollection(
            path.endNode().getRelationships(RelType.PARENT_OF, Direction.OUTGOING))
    );

    return TAX_ORDER.sortedCopy(children);
  }

  @Override
  public PathExpander reverse() {
    reverse = !reverse;
    return this;
  }

}
