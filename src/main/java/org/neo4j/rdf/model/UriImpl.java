package org.neo4j.rdf.model;

/**
 * The default implementation of {@link Uri}.
 */
public class UriImpl implements Uri
{
    private String uri;

    /**
     * @param uri the URI.
     */
    public UriImpl( String uri )
    {
        this.uri = uri;
    }

    public String uriAsString()
    {
        return this.uri;
    }
    
    @Override
    public String toString()
    {
        return "Uri[" + uriAsString() + "]";
    }
}
