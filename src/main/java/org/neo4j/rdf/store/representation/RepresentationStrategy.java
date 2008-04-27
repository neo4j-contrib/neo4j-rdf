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
     * the node space.
     * @param statement the {@link Statement} to make a representation of.
     * @return the abstract representation of {@code statement}.
     */
    AbstractRepresentation getAbstractRepresentation( Statement statement );
    
    /**
     * @return a suitable {@link RepresentationExecutor} which is capable of
     * handling the {@link AbstractRepresentation}s created in
     * {@link #getAbstractRepresentation(Statement)}.
     */
    RepresentationExecutor getExecutor();
}
