package org.neo4j.rdf.store.representation;

import org.neo4j.rdf.model.Statement;

/**
 * Takes a statement and makes a representation of how it would look like in
 * the node space.
 */
public interface RdfRepresentationStrategy
{
    /**
     * Makes a representation of how {@code statement} would look like in
     * the node space.
     * @param statement the {@link Statement} to make a representation of.
     * @return the abstract representation of {@code statement}.
     */
    AbstractStatementRepresentation getAbstractRepresentation(
        Statement statement );
    
    /**
     * @return a suitable {@link AsrExecutor} which is capable of handling
     * the {@link AbstractStatementRepresentation}s created in
     * {@link #getAbstractRepresentation(Statement)}.
     */
    AsrExecutor getAsrExecutor();
}
