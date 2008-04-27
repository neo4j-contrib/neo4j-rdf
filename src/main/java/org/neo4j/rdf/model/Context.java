package org.neo4j.rdf.model;

/**
/**
 * Represents additional context information used in conjunction with
 * {@link Statement}s.
 */
public class Context extends Uri
{
    public static final Context NULL = new Context(
        "http://uri.neo4j.org/the-default-graph" );
    public Context( String contextUri )
    {
        super( contextUri );
    }
    
    @Override
    public String toString()
    {
        return "Context[" + getUriAsString() + "]";
    }
}
