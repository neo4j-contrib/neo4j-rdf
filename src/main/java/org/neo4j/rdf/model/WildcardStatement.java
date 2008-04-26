package org.neo4j.rdf.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WildcardStatement implements Statement
{
    private final Value subject, predicate, object;
    private final List<Context> contextList;

    public WildcardStatement( Value subject, Value predicate,
        Value object, Context... contextsOrNullForNone )
    {
        checkAllowed( subject, predicate, object );
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        if ( contextsOrNullForNone == null )
        {
            this.contextList = Collections.emptyList();
        }
        else
        {
            contextList = Collections.unmodifiableList( Arrays.asList(
                contextsOrNullForNone ) );
        }
    }

    public WildcardStatement( CompleteStatement completeStatement )
    {
        this( completeStatement.getSubject(), completeStatement.getPredicate(),
            completeStatement.getObject(), contextIterableToArray(
                completeStatement.getContexts() ) );
    }

    private static Context[] contextIterableToArray(
        Iterable<Context>  contexts )
    {
        ArrayList<Context> contextList = new ArrayList<Context>();
        for ( Context context : contexts )
        {
            contextList.add( context );
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
}