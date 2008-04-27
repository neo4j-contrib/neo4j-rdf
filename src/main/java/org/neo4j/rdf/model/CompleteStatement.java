package org.neo4j.rdf.model;

/**
 * A complete statement, i.e. a statement which has a non-null subject,
 * predicate, object and a context. It is guaranteed that all values are
 * non-null or wildcards.
 */
public class CompleteStatement implements Statement
{
    private final Resource subject;
    private final Uri predicate;
    private final Value object;
    private final Context context;

    public CompleteStatement( Resource subject, Uri predicate, Resource
        objectResource, Context context )
    {
        this( subject, predicate, ( Value ) objectResource, context );
    }

    public CompleteStatement( Resource subject, Uri predicate, Literal
        objectLiteral, Context context )
    {
        this( subject, predicate, ( Value ) objectLiteral, context );
    }

    private CompleteStatement( Resource subject, Uri predicate, Value object,
        Context context )
    {
        // s, p, o can't be null, context can be null
        assertNotNull( subject, predicate, object, context );
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.context = context;
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

    /**
     * The context of this statement, guaranteed to not be <code>null</code>.
     * It will either return {@link Context#NULL} for the special "default
     * graph" or another {@link Context} instance for a specific named graph.
     * It will never return a {@link Wildcard}.
     * @return the context for this statement
     */    
    public Context getContext()
    {
        return this.context;
    }

    /**
     * Converts this statement to a wildcard statement.
     * @return this statement as a wildcard statement
     */
    public WildcardStatement asWildcardStatement()
    {
        return new WildcardStatement( this );
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
        return value.toString();
    }
}
