package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.WildcardStatement;

/**
 * A RDF/triple store.
 */
public interface RdfStore
{
    /**
     * Adds one or more statements to the store. The statements are added in
     * a single transaction.
     * @param statements the statement to add.
     * @param contexts the additional context information about the statement.
     */
    void addStatements( CompleteStatement... statements );
    
    /**
     * Queries the store for matching statements.
     * @param statement a {@link Statement} with optional wildcard members
     * @param includeInferredStatements wether or not to match f.ex.
     * subclass/subproperty relations.
     * @return the matching statements.
     */
    Iterable<Statement> getStatements( WildcardStatement statement,
        boolean includeInferredStatements );
    
    /**
     * Removes any matching statement from the store.
     * @param statement a {@link Statement} with optional wildcard members
     */
    void removeStatements( Statement statement );
}
