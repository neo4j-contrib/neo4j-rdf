package org.neo4j.rdf.model;

/**
 * Default implementation of {@link Predicate}.
 */
public class PredicateImpl implements Predicate
{
    private String uri;

    /**
     * @param uri the URI.
     */
    public PredicateImpl( String uri )
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
        return "Predicate[" + uriAsString() + "]";
    }
}
