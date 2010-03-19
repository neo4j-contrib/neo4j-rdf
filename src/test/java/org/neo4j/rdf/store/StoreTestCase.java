package org.neo4j.rdf.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.transaction.SystemException;

import org.junit.After;
import org.junit.Before;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.fulltext.SimpleFulltextIndex;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;

public abstract class StoreTestCase extends Neo4jTestCase
{
    public static final String BASE_URI = "http://uri.neo4j.org/";
    public static final Wildcard WILDCARD_SUBJECT = new Wildcard( "subject" );
    public static final Wildcard WILDCARD_PREDICATE= new Wildcard(
        "predicate" );
    public static final Wildcard WILDCARD_OBJECT = new Wildcard( "object" );

    public static final Uri PERSON = new Uri( "http://person" );
    public static final Uri NAME = new Uri( "http://name" );
    public static final Uri NICKNAME = new Uri( "http://nickname" );
    public static final Uri KNOWS = new Uri( "http://knows" );
    public static final Context TEST_CONTEXT = new Context( "aTest" );

    private RdfStore store;
    private FulltextIndex fulltextIndex;
    
    @Before
    public void setUpStore() throws Exception
    {
        fulltextIndex = instantiateFulltextIndex();
        store = instantiateStore();
    }

    @After
    public void tearDownStore() throws Exception
    {
        // TODO, not really nice
        FulltextIndex fulltextIndex =
            ( ( RdfStoreImpl ) this.store ).getFulltextIndex();
        if ( fulltextIndex != null )
        {
            fulltextIndex.clear();
        }
        this.store.shutDown();
    }

    @Override
    protected void restartTx()
    {
        try
        {
            // Temporary solution
            int txId =
                graphDbUtil().getTransactionManager().getTransaction().hashCode();
            super.restartTx();
            
            FulltextIndex fulltextIndex = 
                ( ( RdfStoreImpl ) store ).getFulltextIndex();
            if ( fulltextIndex != null )
            {
                fulltextIndex.end( txId, true );
            }
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected RdfStore store()
    {
        return this.store;
    }
    
    protected FulltextIndex fulltextIndex()
    {
        return this.fulltextIndex;
    }

    protected FulltextIndex instantiateFulltextIndex()
    {
        return new SimpleFulltextIndex( graphDb(), new File( getBasePath(),
            "fulltext" ) );
    }

    protected abstract RdfStore instantiateStore();
    
    protected void waitForFulltextIndex()
    {
        try
        {
            while ( !fulltextIndex().queueIsEmpty() )
            {
                Thread.sleep( 50 );
            }
        }
        catch ( InterruptedException e )
        {
        }
    }

    protected void debug( String text )
    {
        System.out.println( text );
    }

    protected void add( RdfStore store, Statement statement, int numberOfTimes )
    {
        if ( !(statement instanceof CompleteStatement ) )
        {
            throw new IllegalArgumentException(
                "Can only add complete statements " );
        }
        while ( numberOfTimes-- > 0 )
        {
            debug( "addStatement " + statement );
            store.addStatements( ( CompleteStatement ) statement );
        }
    }

    protected void remove( RdfStore store, WildcardStatement statement,
        int numberOfTimes )
    {
        while ( numberOfTimes-- > 0 )
        {
            debug( "removeStatement " + statement );
            store.removeStatements( statement );
        }
    }

    protected Statement statement( String subject, String predicate,
        Resource object, Context context )
    {
        return new CompleteStatement( new Uri( subject ), new Uri( predicate ),
            object, context, null );
    }

    protected Statement statement( String subject, String predicate,
        Object object, Context context )
    {
        return new CompleteStatement( new Uri( subject ), new Uri( predicate ),
            new Literal( object ), context, null );
    }

    protected void removeStatements( RdfStore store,
        List<CompleteStatement> statements )
    {
        removeStatements( store, statements, 1 );
    }

    protected void removeStatements( RdfStore store,
        List<CompleteStatement> statements, int numberOfTimesForEach )
    {
        while ( !statements.isEmpty() )
        {
            CompleteStatement statement = statements.remove(
                new Random().nextInt( statements.size() ) );
            WildcardStatement wildcardStatement =
                statement.asWildcardStatement();
            remove( store, wildcardStatement, numberOfTimesForEach );
        }
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
            assertTrue( "Result found in store, but not in expected list: " +
                resultStatement, foundEquivalentStatement != null );
            expectedResultCollection.remove( foundEquivalentStatement );
        }
        assertTrue( "Statements which should've been found " +
            expectedResultCollection, expectedResultCollection.isEmpty() );
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
    
    protected void removeStatements( WildcardStatement statement )
    {
        store.removeStatements( statement );
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

    static CompleteStatement completeStatement( TestUri subject,
        TestUri predicate, Literal objectLiteral, Context context )
    {
        return new CompleteStatement(
            new Uri( subject.uriAsString() ),
            new Uri( predicate.uriAsString() ),
            objectLiteral,
            context );
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
        TestUri predicate, Literal objectLiteral, TestUri context )
    {
        return new CompleteStatement(
            new Uri( subject.uriAsString() ),
            new Uri( predicate.uriAsString() ),
            objectLiteral,
            new Context( context.uriAsString() ), null );
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
        JOHAN( "person/johan" ),
        FOAF_KNOWS( "foaf_knows" ),
        FOAF_NICK( "foaf_nick" ),
        FOAF_NAME( "foaf_name" ),
        EMIL_PUBLIC_GRAPH( "context/emil-public" ),
        EMIL_PRIVATE_GRAPH( "context/emil-private" ),
        MATTIAS_PUBLIC_GRAPH( "context/mattias-public" ),
        MATTIAS_PRIVATE_GRAPH( "context/mattias-private" ),
        PERSON( "person" ),
        RDF_TYPE( AbstractUriBasedExecutor.RDF_TYPE_URI ),
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
