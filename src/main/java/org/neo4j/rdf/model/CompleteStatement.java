package org.neo4j.rdf.model;

import java.util.LinkedList;
import java.util.List;

/**
 * A complete statement, i.e. a statement without any wildcards.
 */
public class CompleteStatement implements Statement
{
    private final Resource subject;
    private final Uri predicate;
    private final Value object;
    private final List<Context> contextList;

    public CompleteStatement( Resource subject, Uri predicate, Resource
        objectResource, Context mandatoryContext, Context... optionalContexts )
    {
        this( subject, predicate, ( Value ) objectResource, mandatoryContext,
            optionalContexts );
    }

    public CompleteStatement( Resource subject, Uri predicate, Literal
        objectLiteral, Context mandatoryContext, Context... optionalContexts )
    {
        this( subject, predicate, ( Value ) objectLiteral, mandatoryContext,
            optionalContexts );
    }

    private CompleteStatement( Resource subject, Uri predicate, Value object,
        Context mandatoryContext, Context... optionalContexts )
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.contextList = new LinkedList<Context>();
        this.contextList.add( mandatoryContext );
        for ( Context context : optionalContexts )
        {
            this.contextList.add( context );
        }
    }

    public Resource getSubject()
    {
        return this.subject;
    }

    public Uri getPredicate()
    {
        return this.predicate;
    }

    /**
     * Returns the literal or resource object of this statement, guaranteed
     * to not be a wildcard.
     */
    public Value getObject()
    {
        return this.object;
    }

    public Iterable<Context> getContexts()
    {
        return this.contextList;
    }

    /**
     * Convert this statement to a wildcard statement.
     * @return this statement as a wildcard statement
     */
    public WildcardStatement asWildcardStatement()
    {
        return new WildcardStatement( this );
    }

    @Override
    public String toString()
    {
        StringBuffer contexts = new StringBuffer();
        for ( Context context : getContexts() )
        {
            if ( contexts.length() > 0 )
            {
                contexts.append( "," );
            }
            contexts.append( context == null ? "null" :
                context.getUriAsString() );
        }
        return "s,p,o=[" +
            labelify( getSubject() ) + ", " +
            labelify( getPredicate() ) + ", " +
            labelify( getObject() ) + "] (" +
            contexts.toString() + ")";
    }

    private String labelify( Value value )
    {
        return value.toString();
    }
}
