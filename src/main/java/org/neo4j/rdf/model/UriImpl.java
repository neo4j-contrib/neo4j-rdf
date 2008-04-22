package org.neo4j.rdf.model;

public class UriImpl implements Uri
{
    private String uri;

    public UriImpl( String uri )
    {
        this.uri = uri;
    }

    public String uriAsString()
    {
        return this.uri;
    }
}
