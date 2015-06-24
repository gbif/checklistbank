package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.RelType;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;

/**
 * Various reusable traversal descriptions for taxonomic neo dbs.
 */
public class Traversals {
  public static final TraversalDescription PARENT = new MonoDirectionalTraversalDescription()
    .relationships(RelType.PARENT_OF, Direction.INCOMING)
    .depthFirst()
    .evaluator(Evaluators.toDepth(1))
    .evaluator(Evaluators.excludeStartPosition());

  public static final TraversalDescription PARENTS = new MonoDirectionalTraversalDescription()
    .relationships(RelType.PARENT_OF, Direction.INCOMING)
    .depthFirst()
    .evaluator(Evaluators.excludeStartPosition());

  public static final TraversalDescription CHILDREN = new MonoDirectionalTraversalDescription()
    .relationships(RelType.PARENT_OF, Direction.OUTGOING)
    .breadthFirst()
    .evaluator(Evaluators.toDepth(1))
    .evaluator(Evaluators.excludeStartPosition());

  /**
   * Traversal that iterates depth first over all descendants including syononyms.
   */
  public static final TraversalDescription DESCENDANTS = new MonoDirectionalTraversalDescription()
    .relationships(RelType.PARENT_OF, Direction.OUTGOING)
    .relationships(RelType.SYNONYM_OF, Direction.INCOMING)
    .depthFirst()
    .evaluator(Evaluators.excludeStartPosition());

  public static final TraversalDescription SYNONYMS = new MonoDirectionalTraversalDescription()
    .relationships(RelType.SYNONYM_OF, Direction.INCOMING)
    .breadthFirst()
    .evaluator(Evaluators.toDepth(1))
    .evaluator(Evaluators.excludeStartPosition());

  /**
   * Traversal that iterates over all accepted child taxa in taxonomic order, i.e. by rank and secondary ordered by the name
   */
  public static final TraversalDescription ACCEPTED_DESCENDANTS = new MonoDirectionalTraversalDescription()
    .depthFirst()
    .expand(new TaxonomicOrderExpander())
    .evaluator(new AcceptedOnlyEvaluator());

}
