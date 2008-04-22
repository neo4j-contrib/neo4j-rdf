package org.neo4j.rdf.model;

public class PredicateImpl implements Predicate
{
    private String uri;

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
