package org.neo4j.rdf.newmodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WildcardEnabledStatement implements Statement
{
    private final Value subject, predicate, object;
    private final List<Context> contextList;
    
    public WildcardEnabledStatement( Value subject, Value predicate,
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
    
    private void checkAllowed( Value subject, Value predicate, Value object )
    {
        if ( subject == null || predicate == null || object == null )
        {
            throw new IllegalArgumentException( "Null params not allowed" );
        }
        
        if ( predicate instanceof Wildcard )
        {
            throw new IllegalArgumentException(
                "We don't support predicate wildcards " );
        }
        
        if ( subject instanceof Wildcard && object instanceof Wildcard )
        {
            throw new IllegalArgumentException( "We don't support both" +
            	"subject and object being wildcards" );
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
}