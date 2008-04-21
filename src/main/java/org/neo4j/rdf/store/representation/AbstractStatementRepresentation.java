package org.neo4j.rdf.store.representation;

/**
 * An abstract representation of a specific RDF statement in the node space.
 * It is abstract in the sense that it doesn't encapsulate "real" Neo nodes
 * but instead {@link AbstractNode abstract nodes} which are purely in-memory.
 * However, an instance of this type doesn't purely describe the structure
 * of some statements in general, it represents the structure and properties of
 * a <i>specific</i> statement, i.e. the nodes and relationships are actually
 * populated with real values.
 */
public interface AbstractStatementRepresentation
{
    /**
     * Returns the nodes that build up this statement in the node space 
     * @return the nodes that build up this statement in the node space
     */
    Iterable<AbstractNode> nodes();
    /**
     * Returns the relationships that connect the nodes of this statement in
     * the node space.<br>
     * Invariant: the nodes at both ends of each relationship in this set
     * are guaranteed to be part of the set of nodes returned by
     * {@link #nodes()}.
     * @return the relationships that connect this representation of a statement
     */
    Iterable<AbstractRelationship> relationships();
}
