package org.neo4j.rdf.store;

import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;

/**
 * A RDF/triple store.
 */
public interface RdfStore
{
    /**
     * Adds a statement to the store, with optional additional contexts.
     * @param statement the statement to add.
     * @param contexts the additional context information about the statement.
     */
    void addStatement( Statement statement, Context... contexts );
    
    /**
     * Queries the store for matching statements.
     * @param statementWithOptionalNulls a {@link Statement} where the
     * elements may be null (which means wildcard).
     * @param includeInferredStatements wether or not to match f.ex.
     * subclass/subproperty relations.
     * @param contexts the additional context information about the statement.
     * @return the matching statements.
     */
    Iterable<Statement> getStatements( Statement statementWithOptionalNulls,
        boolean includeInferredStatements, Context... contexts );
    
    /**
     * Removes any matching statement from the store.
     * @param statementWithOptionalNulls a {@link Statement} where the
     * elements may be null (which means wildcard).
     * @param contexts the additional context information about the statement.
     */
    void removeStatements( Statement statementWithOptionalNulls,
        Context... contexts );
}
