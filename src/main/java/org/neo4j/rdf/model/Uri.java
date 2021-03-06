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

    /**
     * Returns <code>false</code> (a Uri is not a wildcard).
     * @return <code>false</code>
     */
    public boolean isWildcard()
    {        
        return false;
    }   

    @Override
    public int hashCode()
    {
        return uriAsString.hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o instanceof Uri )
        {
            return getUriAsString().equals( ( ( Uri ) o ).getUriAsString() );
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "Uri[" + uriAsString + "]";
    }
}
