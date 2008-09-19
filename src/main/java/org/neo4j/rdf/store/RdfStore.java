package org.neo4j.rdf.store;

import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.fulltext.QueryResult;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
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
     */
    void addStatements( CompleteStatement... statements );

    /**
     * Queries the store for matching statements.
     * @param statement a statement with optional wildcard members 
     * @param includeInferredStatements whether to match e.g. subclass/
     * subproperty relations
     * @return the matching statements
     */
    Iterable<CompleteStatement> getStatements( WildcardStatement statement,
        boolean includeInferredStatements );
    
    /**
     * Temporary name, search fulltext (literals). The arguments are sure
     * to change over time.
     * @param query the query, basically just a string with a word or two.
     * @return statements matching the query.
     */
    Iterable<QueryResult> searchFulltext( String query );

    /**
     * Removes any matching statement from the store.
     * @param statement a {@link Statement} with optional wildcard members
     */
    void removeStatements( WildcardStatement statement );
    
    /**
     * Returns the number of statements in a some given contexts.
     * If no contexts are supplied then all contexts are calculated.
     * @param contexts the contexts to include in the calculation.
     * @return the number of statements found.
     */
    int size( Context... contexts );
    
    /**
     * Stops thread a.s.o. If a {@link FulltextIndex} is present then the
     * shutDown method is called on that too.
     */
    void shutDown();
}
