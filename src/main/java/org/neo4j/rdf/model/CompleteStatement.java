package org.neo4j.rdf.model;

import java.util.Arrays;
import java.util.Collections;
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
        objectResource, Context... contextsOrNullForNone )
    {
        this( subject, predicate, ( Value ) objectResource,
            contextsOrNullForNone );
    }
    
    public CompleteStatement( Resource subject, Uri predicate, Literal
        objectLiteral, Context... contextsOrNullForNone )
    {
        this( subject, predicate, ( Value ) objectLiteral,
            contextsOrNullForNone );
    }
    
    private CompleteStatement( Resource subject, Uri predicate, Value object,
        Context... contextsOrNullForNone )
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        if ( contextsOrNullForNone == null )
        {
            this.contextList = Collections.emptyList();
        }
        else
        {
            this.contextList = Collections.unmodifiableList( Arrays.asList(
                contextsOrNullForNone ) );
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
    
    public String toString()
    {
        return "s,p,o=[" +
            labelify( getSubject() ) + ", " +
            labelify( getPredicate() ) + ", " +
            labelify( getObject() ) + "]";
    }

    private String labelify( Value value )
    {
        return value.toString();
    }
}
