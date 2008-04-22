package org.neo4j.rdf.store.representation;

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
}
