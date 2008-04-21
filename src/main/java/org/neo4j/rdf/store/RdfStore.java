package org.neo4j.rdf.store;

import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;

public interface RdfStore
{
    void addStatement( Statement statement, Context... contexts );
    
    Iterable<Statement> getStatements( Statement statementWithOptionalNulls,
        boolean includeInferredStatements, Context... contexts );
    
    void removeStatements( Statement statementWithOptionalNulls,
        Context... contexts );
}
