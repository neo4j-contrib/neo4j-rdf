package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.Node;

/**
 * Given an {@link AbstractStatementRepresentation} an {@link AsrExecutor}
 * can make sure it's added to or removed from the node space.
 */
public interface AsrExecutor
{
    /**
     * Adds a statement representation to the node space if it doesn't exist.
     * @param representation the representation of what is to be added
     * to the node space.
     */
	void addToNodeSpace( AbstractStatementRepresentation representation );
	
    /**
     * Removes a statement representation from the node space if it exists.
     * @param representation the representation of what is to be removed
     * from the node space.
     */
	void removeFromNodeSpace( AbstractStatementRepresentation representation );	
	
	/**
	 * Looks up one {@link AbstractNode} and returns its corresponding real
	 * {@link Node} in the node space.
	 * @param abstractNode the {@link AbstractNode} to get the {@link Node} for.
	 * @return the corresponding {@link Node}.
	 */
	Node lookupNode( AbstractNode abstractNode );
}
