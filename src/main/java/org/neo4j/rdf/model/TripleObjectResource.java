package org.neo4j.rdf.model;

public class TripleObjectResource implements TripleObject
{
    private Uri uri;

    public TripleObjectResource( Uri resourceUri )
    {
        this.uri = resourceUri;
    }

    public Object getLiteralValueOrNull()
    {
        return null;
    }

    public Uri getResourceOrNull()
    {
        return this.uri;
    }

    public boolean isObjectProperty()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "ObjectResource[" + getResourceOrNull().uriAsString() + "]";
    }
}
