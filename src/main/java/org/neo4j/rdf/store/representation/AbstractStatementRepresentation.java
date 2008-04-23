package org.neo4j.rdf.store.representation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract representation of a specific RDF statement in the node space.
 * It is abstract in the sense that it doesn't encapsulate "real" Neo nodes
 * but instead {@link AbstractNode abstract nodes} which are purely in-memory.
 * However, an instance of this type doesn't purely describe the structure
 * of some statements in general, it represents the structure and properties of
 * a <i>specific</i> statement, i.e. the nodes and relationships are actually
 * populated with real values.
 */
public class AbstractStatementRepresentation
{
    private final List<AbstractNode> nodes = new ArrayList<AbstractNode>();
    private final List<AbstractRelationship> relationships =
    	new ArrayList<AbstractRelationship>();
    
    /**
     * Adds an {@link AbstractNode} to the representation.
     * @param node the node to add to the representation.
     */
    public void addNode( AbstractNode node )
    {
        if ( !this.nodes.contains( node ) )
        {
            this.nodes.add( node );
        }
    }
    
    /**
     * Adds an {@link AbstractRelationship} to the representation.
     * @param relationship the relationship to add to the representation.
     */
    public void addRelationship( AbstractRelationship relationship )
    {
        if ( !this.relationships.contains( relationship ) )
        {
            this.relationships.add( relationship );
        }
    }
    
    /**
     * Returns the nodes that build up this statement in the node space 
     * @return the nodes that build up this statement in the node space
     */
    public Iterable<AbstractNode> nodes()
    {
        return Collections.unmodifiableList( nodes );
    }
    
    /**
     * Returns the relationships that connect the nodes of this statement in
     * the node space.<br/>
     * Invariant: the nodes at both ends of each relationship in this set
     * are guaranteed to be part of the set of nodes returned by
     * {@link #nodes()}.
     * @return the relationships that connect this representation of a statement
     */
    public Iterable<AbstractRelationship> relationships()
    {
        return Collections.unmodifiableList( relationships );
    }
}
