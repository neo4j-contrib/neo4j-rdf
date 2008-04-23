package org.neo4j.rdf.model;

/**
 * Represents a URI.
 */
public class Uri implements Resource
{
    private final String uriAsString;
    
    public Uri( String uriAsString )
    {
        if ( uriAsString == null )
        {
            throw new IllegalArgumentException( "The URI string must not be " +
                "null" );
        }
        this.uriAsString = uriAsString; 
    }
    
    /**
     * The URI as a string
     * @return the URI as a string.
     */
    public String getUriAsString()
    {
        return this.uriAsString;
    }

    @Override
    public String toString()
    {
        return "Uri[" + uriAsString + "]";
    }
}
