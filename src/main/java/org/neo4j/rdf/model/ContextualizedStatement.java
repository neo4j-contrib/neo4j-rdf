package org.neo4j.rdf.model;

public interface ContextualizedStatement extends Statement
{
    Context getSingleContextOrNull();
    Iterable<Context> getContexts();
}
