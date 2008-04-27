package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.RelationshipType;

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
