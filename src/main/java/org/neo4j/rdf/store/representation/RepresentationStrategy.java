package org.neo4j.rdf.store.representation;

import org.neo4j.rdf.model.Statement;

/**
 * Takes a statement and makes a representation of how it would look like in
 * the node space.
 */
public interface RepresentationStrategy
{
    /**
     * Makes a representation of how {@code statement} would look like in
     * the node space. The representation from the statement will be merged
     * into {@code representation}.
     * @param statement the {@link Statement} to make a representation of.
     * @return the abstract representation which was passed in, now also filled
     * with the abstract representation of the statement.
     */
    AbstractRepresentation getAbstractRepresentation( Statement statement,
        AbstractRepresentation representation );
    
    /**
     * @return a suitable {@link RepresentationExecutor} which is capable of
     * handling the {@link AbstractRepresentation}s created in
     * {@link #getAbstractRepresentation(Statement)}.
     */
    RepresentationExecutor getExecutor();
}
