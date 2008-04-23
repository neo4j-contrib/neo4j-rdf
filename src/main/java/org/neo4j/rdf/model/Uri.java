package org.neo4j.rdf.model;

public class Uri implements Resource
{
    private final String uriAsString;
    
    public Uri( String uriAsString )
    {
        this.uriAsString = uriAsString; 
    }
    
    public String getUriAsString()
    {
        return this.uriAsString;
    }
}
