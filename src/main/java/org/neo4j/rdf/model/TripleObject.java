package org.neo4j.rdf.model;

public interface TripleObject
{
    boolean isObjectProperty();
    Uri getResourceOrNull();
    Object getLiteralValueOrNull();
}
