package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.util.index.IndexService;
import org.neo4j.util.index.NeoIndexService;

public abstract class QuadStoreAbstractTestCase extends NeoTestCase
{
    public static final String BASE_URI = "http://uri.neo4j.org/";
    public static final Wildcard WILDCARD_SUBJECT = new Wildcard( "subject" );
    public static final Wildcard WILDCARD_PREDICATE= new Wildcard(
        "predicate" );
    public static final Wildcard WILDCARD_OBJECT = new Wildcard( "object" );
    public static final Wildcard WILDCARD_CONTEXT = new Wildcard( "context" );
    

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

    protected void assertResultCount( WildcardStatement wildcard,
        int expectedCount )
    {
        Iterator<CompleteStatement> result =
            store().getStatements( wildcard, false ).iterator();
        int resultCount = 0;
        while ( result.hasNext() )
        {
            result.next();
            resultCount++;
        }
        assertEquals( expectedCount, resultCount );
    }

    protected void assertResult( WildcardStatement wildcard,
        CompleteStatement... expectedResult )
    {
        Collection<CompleteStatement> expectedResultCollection =
            new ArrayList<CompleteStatement>( Arrays.asList( expectedResult ) );
        Iterable<CompleteStatement> result =
            store().getStatements( wildcard, false );
        for ( CompleteStatement resultStatement : result )
        {
            CompleteStatement foundEquivalentStatement = null;
            for ( CompleteStatement expectedResultStatement :
                expectedResultCollection )
            {
                if ( statementsAreEquivalent( resultStatement,
                    expectedResultStatement ) )
                {
                    foundEquivalentStatement = expectedResultStatement;
                    break;
                }
            }
            assertTrue( "Result found in store, but in expected list: " +
                resultStatement, foundEquivalentStatement != null );
            expectedResultCollection.remove( foundEquivalentStatement );
        }
        assertTrue( expectedResultCollection.isEmpty() );
    }

    protected void assertEquivalentStatement( Statement first,
        Statement second )
    {
        assertTrue( statementsAreEquivalent( first, second ) );
    }

    protected boolean statementsAreEquivalent( Statement first,
        Statement second )
    {
        return first.getSubject().equals( second.getSubject() ) &&
            first.getPredicate().equals( second.getPredicate() ) &&
            first.getObject().equals( second.getObject() ) &&
            first.getContext().equals( second.getContext() );
    }

    protected void addStatements( CompleteStatement... statements )
    {
        store().addStatements( statements );
    }

    static CompleteStatement completeStatement( TestUri subject,
        TestUri predicate, TestUri object, TestUri context )
    {
        return completeStatement( subject.uriAsString(),
            predicate.uriAsString(), object.uriAsString(),
            context.uriAsString() );
    }

    static CompleteStatement completeStatement( TestUri subject,
        TestUri predicate, TestUri object, Context context )
    {
        return completeStatement( subject.uriAsString(),
            predicate.uriAsString(), object.uriAsString(),
            context.getUriAsString() );
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

    static WildcardStatement wildcardStatement( TestUri subject, TestUri
        predicate, TestUri object, TestUri context )
    {
        return wildcardStatement( subject.toUri(), predicate.toUri(),
            object.toUri(), new Context( context.uriAsString() ) );
    }

    static WildcardStatement wildcardStatement( TestUri subject, TestUri
        predicate, TestUri object, Value context )
    {
        return wildcardStatement( subject.toUri(), predicate.toUri(),
            object.toUri(), context );
    }

    static WildcardStatement wildcardStatement( Value subject, Value predicate,
        Value object, Value context )
    {
        return new WildcardStatement( subject, predicate, object, context );
    }

    public enum TestUri
    {
        EMIL( "person/emil" ),
        MATTIAS( "person/mattias" ),
        FOAF_KNOWS( "foaf:knows" ),
        FOAF_NICK( "foaf:nick" ),
        EMIL_PUBLIC_GRAPH( "context/emil-public" ),
        EMIL_PRIVATE_GRAPH( "context/emil-private" ),
        MATTIAS_PUBLIC_GRAPH( "context/mattias-public" ),
        MATTIAS_PRIVATE_GRAPH( "context/mattias-private" ),
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
