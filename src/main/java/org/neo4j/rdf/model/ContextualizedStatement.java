package org.neo4j.rdf.model;

/**
 * A {@link Statement} with additional {@link Context}s associated with it.
 */
public interface ContextualizedStatement extends Statement
{
    /**
     * @return the single associated {@link Context} if there's exactely one or
     * {@code null} if there's none.
     * @throws RuntimeException if there's more than one {@link Context}s
     * associated.
     */
    Context getSingleContextOrNull();
    
    /**
     * @return all the contexts associated with this statement.
     */
    Iterable<Context> getContexts();
}
