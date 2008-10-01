package org.neo4j.rdf.model;

import org.neo4j.rdf.store.RdfStore;
import org.neo4j.rdf.store.representation.RepresentationStrategy;

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
    private final StatementMetadata metadata;

    public CompleteStatement( Resource subject, Uri predicate, Resource
        objectResource, Context context )
    {
        this( subject, predicate, objectResource, context, null );
    }
    
    public CompleteStatement( Resource subject, Uri predicate, Resource
        objectResource, Context context, StatementMetadata metadata )
    {
        this( subject, predicate, ( Value ) objectResource, context, metadata );
    }

    public CompleteStatement( Resource subject, Uri predicate, Literal
        objectLiteral, Context context )
    {
        this( subject, predicate, objectLiteral, context, null );
    }
    
    public CompleteStatement( Resource subject, Uri predicate, Literal
        objectLiteral, Context context, StatementMetadata metadata )
    {
        this( subject, predicate, ( Value ) objectLiteral, context, metadata );
    }

    private CompleteStatement( Resource subject, Uri predicate, Value object,
        Context context, StatementMetadata metadata )
    {
        // metadata can be null, this is wether or not the RdfStore instance
        // supports metadata on statments or not.
        assertNotNull( subject, predicate, object, context );
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.context = context;
        this.metadata = metadata;
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

    /**
     * Returns the subject of this statement, guaranteed not to be a wildcard.
     */
    public Resource getSubject()
    {
        return this.subject;
    }

    /**
     * Returns the predicate of this statement, guaranteed not to be a wildcard.
     */
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
    
    /**
     * Returns a hook for reading and editing metadata for this statement.
     * This hook is supplied in the constructor, typically by an
     * {@link RdfStore} instance. The {@link RdfStore} can also supply null,
     * meaning that the particular store instance (or more correctly the
     * {@link RepresentationStrategy} it uses) doesn't support metadata for
     * statements.
     * @return a metadata hook for this statement.
     */
    public StatementMetadata getMetadata()
    {
        if ( this.metadata == null )
        {
            throw new RuntimeException( "You can't associate metadata to " +
                "this statement. Normally you can only associate metadata to " +
                "statements reached via getStatements method from an " +
                RdfStore.class.getSimpleName() );
        }
        return this.metadata;
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
