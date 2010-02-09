package org.neo4j.rdf.store.representation;

import org.neo4j.graphdb.Node;

/**
 * Given an {@link AbstractRepresentation} a {@link RepresentationExecutor} can
 * make sure it's added to or removed from the node space.
 */
public interface RepresentationExecutor
{
    /**
     * Adds a statement representation to the node space if it doesn't exist.
     * @param representation the representation of what is to be added
     * to the node space.
     */
    void addToNodeSpace( AbstractRepresentation representation );
    
    /**
     * Removes a statement representation from the node space if it exists.
     * @param representation the representation of what is to be removed
     * from the node space.
     */
    void removeFromNodeSpace( AbstractRepresentation representation );	
    
    /**
     * Looks up one {@link AbstractNode} and returns its corresponding real
     * {@link Node} in the node space.
     * @param abstractNode the {@link AbstractNode} to get the {@link Node} for.
     * @return the corresponding {@link Node}.
     */
    Node lookupNode( AbstractNode abstractNode );
    
    /**
     * @param abstractNode the {@link AbstractNode} which represents the
     * {@link Node}.
     * @return the property key on the {@link Node} which represents the URI
     * of the node.
     */
    String getNodeUriPropertyKey( AbstractNode abstractNode );
}
