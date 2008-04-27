package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.util.index.IndexService;
import org.neo4j.util.index.NeoIndexService;

public abstract class QuadStoreAbstractTestCase extends NeoTestCase
{
    public static final String BASE_URI = "http://uri.neo4j.org/";
    private RdfStore store = null;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        createStoreIfNeeded();
    }

    @Override
    protected void tearDown() throws Exception
    {
        tearDownStoreIfNeeded();
        super.tearDown();
    }

    private void createStoreIfNeeded()
    {
        if ( store() == null )
        {
            this.store = instantiateStore();
        }
    }

    private void tearDownStoreIfNeeded()
    {
        if ( store() != null )
        {
            this.store = null;
        }
    }

    protected RdfStore store()
    {
        return this.store;
    }

    @Override
    protected IndexService instantiateIndexService()
    {
        return new NeoIndexService( neo() );
    }

    protected abstract RdfStore instantiateStore();

    protected void debug( String text )
    {
        System.out.println( text );
    }

    protected void addStatements( CompleteStatement... statements )
    {
        store().addStatements( statements );
    }

    static CompleteStatement completeStatement( TestUri subject,
        TestUri predicate, TestUri object, TestUri context )
    {
        return completeStatement( subject.uriAsString(), predicate.uriAsString(), object.uriAsString(),
            context.uriAsString() );
    }

    static CompleteStatement completeStatement( String subjectUri,
        String predicateUri, String objectUri, String contextUri )
    {
        return new CompleteStatement(
            new Uri( subjectUri ),
            new Uri( predicateUri ),
            new Uri( objectUri ),
            new Context( contextUri ) );
    }

    static CompleteStatement completeStatement( TestUri subject,
        TestUri predicate, Object objectLiteral, TestUri context )
    {
        assertFalse( objectLiteral instanceof TestUri );
        return new CompleteStatement(
            new Uri( subject.uriAsString() ),
            new Uri( predicate.uriAsString() ),
            new Literal( objectLiteral ),
            new Context( context.uriAsString() ) );
    }

    static WildcardStatement wildcardStatement( Value subject, Value predicate,
        Value object, Value context )
    {
        return null;

    }

    public enum TestUri
    {
        EMIL( "person/emil" ),
        MATTIAS( "person/mattias" ),
        FOAF_KNOWS( "foaf:knows" ),
        EMIL_PUBLIC_GRAPH( "context/emil-public" ),
        EMIL_PRIVATE_GRAPH( "context/emil-private" ),
        MATTIAS_PUBLIC_GRAPH( "context/mattias-public" ),
        MATTIAS_PRIVATE_GRAPH( "context/mattias-private" ),
        NULL_CONTEXT( Context.NULL.getUriAsString() ),
        ;

        private final String uri;
        TestUri( String uri )
        {
            if ( uri.startsWith( "http://" ) || uri.contains( ":" ) )
            {
                this.uri = uri;
            }
            else
            {
                this.uri = BASE_URI + uri;
            }
            System.out.println( "My name is " + this.name() + " and my URI " +
                "is " + uri );
        }

        public String uriAsString()
        {
            return this.uri;
        }

        public Uri toUri()
        {
            return new Uri( uriAsString() );
        }

        @Override
        public String toString()
        {
            return uriAsString();
        }
    }
}
