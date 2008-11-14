package org.neo4j.rdf.store.representation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.rdf.model.Value;

/**
 * An abstract representation of an arbitrary number of RDF statements in the
 * node space. It is created by a {@link RepresentationStrategy}. An instance
 * of this type is abstract in the sense that it doesn't encapsulate "real"
 * Neo nodes but instead {@link AbstractNode abstract nodes} which are purely
 * in-memory. It's important to remember that it's not just a description of the 
 * structure of some statements in general, but rather it represents the
 * structure and properties of some <i>specific</i> statements, i.e. the nodes
 * and relationships are actually populated with real values.
 */
public class AbstractRepresentation
{
    private final Map<Object, AbstractNode> nodeKeys =
        new HashMap<Object, AbstractNode>();
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
            if ( node.getKey() != null )
            {
                this.nodeKeys.put( node.getKey(), node );
            }
        }
    }
    
    public AbstractNode node( Object key )
    {
        return nodeKeys.get( key );
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

    // Maybe add this?
    public Map<AbstractElement, Value> getMappingToRdfValues()
    {
        return Collections.emptyMap();
    }
}
