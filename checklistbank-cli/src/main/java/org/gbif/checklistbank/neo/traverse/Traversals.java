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
     * Traversal that iterates depth first over all descendants including synonyms.
     */
    public static final TraversalDescription DESCENDANTS = new MonoDirectionalTraversalDescription()
            .relationships(RelType.PARENT_OF, Direction.OUTGOING)
            .relationships(RelType.SYNONYM_OF, Direction.INCOMING)
            .relationships(RelType.PROPARTE_SYNONYM_OF, Direction.INCOMING)
            .depthFirst()
            .evaluator(Evaluators.excludeStartPosition());

    public static final TraversalDescription SYNONYMS = new MonoDirectionalTraversalDescription()
            .relationships(RelType.SYNONYM_OF, Direction.INCOMING)
            .breadthFirst()
            .evaluator(Evaluators.toDepth(1))
            .evaluator(Evaluators.excludeStartPosition());

    public static final TraversalDescription ACCEPTED = new MonoDirectionalTraversalDescription()
            .relationships(RelType.SYNONYM_OF, Direction.OUTGOING)
            .relationships(RelType.PROPARTE_SYNONYM_OF, Direction.OUTGOING)
            .breadthFirst()
            .evaluator(Evaluators.toDepth(1))
            .evaluator(Evaluators.excludeStartPosition());

    /**
     * Finds all nodes connected via a basionym_of relation regardless of the direction.
     */
    public static final TraversalDescription BASIONYM_GROUP = new MonoDirectionalTraversalDescription()
            .relationships(RelType.BASIONYM_OF)
            .breadthFirst()
            .evaluator(Evaluators.excludeStartPosition());

    /**
     * Traversal that iterates over all accepted child taxa in taxonomic order, i.e. by rank and secondary ordered by the name.
     * The traversal includes the initial starting node!
     */
    public static final TraversalDescription ACCEPTED_DESCENDANTS = new MonoDirectionalTraversalDescription()
            .depthFirst()
            .expand(new TaxonomicOrderExpander())
            .evaluator(new AcceptedOnlyEvaluator());

}
