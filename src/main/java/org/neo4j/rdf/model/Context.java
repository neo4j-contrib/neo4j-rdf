package org.neo4j.rdf.model;

/**
/**
 * Represents additional context information used in conjunction with
 * {@link Statement}s.
 */
public class Context extends Uri
{
    public Context( String contextUri )
    {
        super( contextUri );
    }
}
