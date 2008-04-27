package org.neo4j.rdf.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WildcardStatement implements Statement
{
    private final Value subject, predicate, object;
    private final List<Context> contextList;

    public WildcardStatement( Value subject, Value predicate,
        Value object, Context mandatoryContext, Context... optionalContexts )
    {
        checkAllowed( subject, predicate, object );
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

    public WildcardStatement( CompleteStatement completeStatement )
    {
        this( completeStatement.getSubject(), completeStatement.getPredicate(),
            completeStatement.getObject(), firstContext( completeStatement ),
            restOfContexts( completeStatement ) );
    }
    
    private static Context firstContext( CompleteStatement statement )
    {
        return statement.getContexts().iterator().next();
    }
    
    private static Context[] restOfContexts( CompleteStatement statement )
    {
        ArrayList<Context> contextList = new ArrayList<Context>();
        Iterator<Context> it = statement.getContexts().iterator();
        it.next();
        while ( it.hasNext() )
        {
            contextList.add( it.next() );
        }
        return contextList.toArray( new Context[ contextList.size() ] );        
    }

    private void checkAllowed( Value subject, Value predicate, Value object )
    {
        if ( subject == null || predicate == null || object == null )
        {
            throw new IllegalArgumentException( "Null params not allowed" );
        }
    }

    public Value getSubject()
    {
        return this.subject;
    }

    public Value getPredicate()
    {
        return this.predicate;
    }

    public Value getObject()
    {
        return this.object;
    }

    public Iterable<Context> getContexts()
    {
        return this.contextList;
    }

    @Override
    public String toString()
    {
        return "s,p,o=[" +
            labelify( getSubject() ) + ", " +
            labelify( getPredicate() ) + ", " +
            labelify( getObject() ) + "]";
    }

    private String labelify( Value value )
    {
        return value instanceof Wildcard ? "?" : value.toString();
    }

    public WildcardStatement asWildcardStatement()
    {
        return this;
    }
}