package org.neo4j.rdf.model;

public interface Statement
{
    Subject getSubject();
    Predicate getPredicate();
    TripleObject getObject();
}
