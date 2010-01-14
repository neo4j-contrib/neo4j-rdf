package org.neo4j.rdf.store.representation.standard;

import org.neo4j.graphdb.RelationshipType;

public class RelationshipTypeImpl implements RelationshipType
{
    private String name;

    public RelationshipTypeImpl( String name )
    {
        this.name = name;
    }

    public String name()
    {
        return this.name;
    }
}
