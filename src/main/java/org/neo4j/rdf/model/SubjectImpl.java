package org.neo4j.rdf.model;

/**
 * The default implementation of {@link Subject}.
 */
public class SubjectImpl implements Subject
{
    private String uri;

    /**
     * @param uriOrNull the URI of this subject or {@code null} if this
     * subject has no URI.
     */
    public SubjectImpl( String uriOrNull )
    {
        this.uri = uriOrNull;
    }

    public String uriAsString()
    {
        return this.uri;
    }

    @Override
    public String toString()
    {
        return "Subject[" + uriAsString() + "]";
    }
}
