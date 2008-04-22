package org.neo4j.rdf.store.representation;

import org.neo4j.rdf.model.Statement;

public interface RdfRepresentationStrategy
{
    AbstractStatementRepresentation getAbstractRepresentation(
        Statement statement );
    
    AsrExecutor getAsrExecutor();
}
