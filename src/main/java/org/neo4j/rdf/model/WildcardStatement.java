package org.neo4j.rdf.model;

public class WildcardStatement implements Statement
{
    private final Value subject, predicate, object, context;
    
    public WildcardStatement( Value subject, Value predicate,
        Value object, Value context )
    {
        assertNotNull( subject, predicate, object, context );
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.context = context;
    }

    public WildcardStatement( CompleteStatement completeStatement )
    {
        this( completeStatement.getSubject(), completeStatement.getPredicate(),
            completeStatement.getObject(), completeStatement.getContext() );
    }
    
    private void assertNotNull( Object... args )
    {
        for ( Object arg : args )
        {
            if ( arg == null )
            {
                throw new IllegalArgumentException( "Null argument not valid" );
            }
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
    
    public Value getContext()
    {
        return this.context;
    }

    @Override
    public String toString()
    {
        return "s,p,o=[" +
            labelify( getSubject() ) + ", " +
            labelify( getPredicate() ) + ", " +
            labelify( getObject() ) + "] (" +
            labelify( getContext() ) + ")";
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