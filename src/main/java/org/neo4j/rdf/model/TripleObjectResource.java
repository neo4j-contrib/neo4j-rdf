package org.neo4j.rdf.model;

/**
 * Implementation of {@link TripleObject} as a resource.
 */
public class TripleObjectResource implements TripleObject
{
    private Uri uri;

    /**
     * @param resourceUri the resource URI.
     */
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
