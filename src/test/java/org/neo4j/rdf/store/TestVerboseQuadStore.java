package org.neo4j.rdf.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.neo4j.rdf.fulltext.QueryResult;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;

public class TestVerboseQuadStore extends QuadStoreAbstractTestCase
{
    @Test
    public void testSameSameStatements() throws Exception
    {
        Uri uriA = new Uri( BASE_URI + "uriA" );
        Uri uriB = new Uri( BASE_URI + "uriB" );
        Uri uriC = new Uri( BASE_URI + "uriC" );
        Uri uriD = new Uri( BASE_URI + "uriD" );
        Uri[] uris = { uriA, uriB, uriC, uriD };
        
        Collection<CompleteStatement> statements =
            new ArrayList<CompleteStatement>();
        int counter = 0;
        for ( int a = 0; a < uris.length; a++ )
        {
            for ( int b = 0; b < uris.length; b++ )
            {
                for ( int c = 0; c < uris.length; c++ )
                {
                    for ( int d = 0; d < uris.length; d++ )
                    {
                        CompleteStatement statement = new CompleteStatement(
                            uris[ a ], uris[ b ], uris[ c ],
                            new Context( uris[ d ].getUriAsString() ) );
                        statements.add( statement );
                        addStatements( statement );
                        counter++;
                    }
                }
            }
        }
        
        WildcardStatement wildcardStatement = wildcardStatement(
            new Wildcard( "s" ), new Wildcard( "p" ), new Wildcard( "o" ),
            new Wildcard( "g" ) );
        assertResult( wildcardStatement, statements.toArray(
            new CompleteStatement[ 0 ] ) );
        removeStatements( wildcardStatement );
    }

    @Test
    public void testIt()
    {
        CompleteStatement mattiasKnowsEmilPublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_KNOWS,
                TestUri.EMIL,
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNamePublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                new Literal( "Mattias Persson",
                    new Uri( "http://www.w3.org/2001/XMLSchema#string" ), "sv" ),
                    TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNamePrivate =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                new Literal( "Mattias Persson",
                    new Uri( "http://www.w3.org/2001/XMLSchema#string" ), "en" ),
                    TestUri.MATTIAS_PRIVATE_GRAPH );
        CompleteStatement mattiasKnowsEmilPrivate =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_KNOWS,
                TestUri.EMIL,
                TestUri.MATTIAS_PRIVATE_GRAPH );
        
        addStatements(
            mattiasKnowsEmilPublic,
            mattiasKnowsEmilPrivate,
            mattiasNamePublic,
            mattiasNamePrivate );
        
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                new Wildcard( "p" ),
                new Wildcard( "o" ),
                new Context( TestUri.MATTIAS_PUBLIC_GRAPH.toUri().
                    getUriAsString() ) ),
                    
            mattiasKnowsEmilPublic,
            mattiasNamePublic );
        
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                new Wildcard( "p" ),
                new Wildcard( "o" ),
                new Wildcard( "g" ) ),
                
            mattiasKnowsEmilPublic,
            mattiasKnowsEmilPrivate,
            mattiasNamePublic,
            mattiasNamePrivate );
        
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                TestUri.FOAF_KNOWS.toUri(),
                new Wildcard( "o" ),
                new Wildcard( "g" ) ),
                
            mattiasKnowsEmilPublic,
            mattiasKnowsEmilPrivate );
        
        deleteEntireNodeSpace();
    }
    
    @Test
    public void testType() throws Exception
    {
        Uri rdfType = new Uri( AbstractUriBasedExecutor.RDF_TYPE_URI );
        CompleteStatement mattiasKnowsEmilPublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_KNOWS,
                TestUri.EMIL,
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasTypePerson =
            new CompleteStatement(
                TestUri.MATTIAS.toUri(),
                rdfType,
                TestUri.PERSON.toUri(),
                Context.NULL, null );
        addStatements(
            mattiasTypePerson,
            mattiasKnowsEmilPublic );
        
        assertResult(
            wildcardStatement(
                TestUri.MATTIAS.toUri(),
                rdfType,
                new Wildcard( "o" ),
                new Wildcard( "g" ) ),
                
            mattiasTypePerson );
        
        deleteEntireNodeSpace();
    }
    
    @Test
    public void testDuplicateWithLiterals() throws Exception
    {
        CompleteStatement mattiasTypePerson =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.RDF_TYPE,
                TestUri.PERSON,
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNamePublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                new Literal( "Mattias Persson" ),
                TestUri.MATTIAS_PUBLIC_GRAPH );
        
        addStatements( mattiasTypePerson, mattiasNamePublic );
        
        restartTx();
        
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                new Wildcard( "p" ),
                new Literal( "Mattias Persson" ),
                new Wildcard( "g" ) ),
                
            mattiasNamePublic );
        deleteEntireNodeSpace();
    }
    
    @Test
    public void testFulltextSearch() throws Exception
    {
        CompleteStatement mattiasTypePerson =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.RDF_TYPE,
                TestUri.PERSON,
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNamePublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                new Literal( "Persson" ),
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNickPublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NAME,
                new Literal( "Mattias Persson" ),
                TestUri.MATTIAS_PUBLIC_GRAPH );
        
        addStatements( mattiasTypePerson, mattiasNamePublic,
            mattiasNickPublic );
        
        restartTx();
        waitForFulltextIndex();
        
        Iterable<QueryResult> queryResult =
            this.store().searchFulltextWithSnippets( " Mattias  Persson", 100 );
        int counter = 0;
        for ( QueryResult oneResult : queryResult )
        {
            counter++;
            assertTrue( oneResult.getSnippet().contains( "Persson" ) );
        }
        assertEquals( 2, counter );
        
        queryResult = this.store().searchFulltextWithSnippets(
            "  \t \n Mattias  AND\tPersson \t    ", 100 );
        counter = 0;
        for ( QueryResult oneResult : queryResult )
        {
            counter++;
            assertEquals( "Mattias Persson", ( ( Literal )
                oneResult.getStatement().getObject() ).getValue() );
            assertTrue( oneResult.getSnippet().contains( "Mattias" ) );
            assertTrue( oneResult.getSnippet().contains( "Persson" ) );
        }
        assertEquals( 1, counter );

        queryResult = this.store().searchFulltextWithSnippets( "\tpersson  ",
            100 );
        counter = 0;
        for ( QueryResult oneResult : queryResult )
        {
            counter++;
            assertTrue( oneResult.getSnippet().contains( "Persson" ) );
        }
        assertEquals( 2, counter );
        
        queryResult = this.store().searchFulltextWithSnippets( "\tpers*  ",
            100 );
        counter = 0;
        for ( QueryResult oneResult : queryResult )
        {
            counter++;
            System.out.println( "snippet '" + oneResult.getSnippet() + "'" +
                ", " + oneResult.getStatement() );
            assertTrue( oneResult.getSnippet().contains( "Persson" ) );
        }
        assertEquals( 2, counter );
        
        removeStatements( new WildcardStatement( mattiasNamePublic ) );
        removeStatements( new WildcardStatement( mattiasNickPublic ) );
        restartTx();
        
        waitForFulltextIndex();
        assertFalse( store().searchFulltext( "Persson" ).iterator().hasNext() );
        
        deleteEntireNodeSpace();
    }
    
    @Test
    public void testReindexFulltextIndex() throws Exception
    {
        CompleteStatement mattiasTypePerson =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.RDF_TYPE,
                TestUri.PERSON,
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNickPublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                new Literal( "Matte" ),
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNamePublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NAME,
                new Literal( "Mattias Persson" ),
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement emilNamePublic =
            completeStatement(
                TestUri.EMIL,
                TestUri.FOAF_NAME,
                new Literal( "Emil Eifrém" ),
                TestUri.EMIL_PUBLIC_GRAPH );
        
        addStatements( mattiasTypePerson, mattiasNickPublic,
            mattiasNamePublic, emilNamePublic );
        restartTx();
        waitForFulltextIndex();
        
        ( ( RdfStoreImpl ) store() ).getFulltextIndex().clear();
        assertFalse( store().searchFulltext( "Persson" ).iterator().hasNext() );
        ( ( RdfStoreImpl ) store() ).reindexFulltextIndex();
        waitForFulltextIndex();
        Iterable<QueryResult> queryResult =
            this.store().searchFulltext( "Persson" );
        int counter = 0;
        for ( QueryResult oneResult : queryResult )
        {
            counter++;
            assertEquals( "Mattias Persson", ( ( Literal )
                oneResult.getStatement().getObject() ).getValue() );
        }
        assertEquals( 1, counter );
        
        queryResult = this.store().searchFulltext( "Emil" );
        counter = 0;
        for ( QueryResult oneResult : queryResult )
        {
            counter++;
            assertEquals( "Emil Eifrém", ( ( Literal )
                oneResult.getStatement().getObject() ).getValue() );
        }
        assertEquals( 1, counter );
        
        deleteEntireNodeSpace();
    }
    
    @Test
    public void testMetadata() throws Exception
    {
        CompleteStatement mattiasTypePerson =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.RDF_TYPE,
                TestUri.PERSON,
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNickPublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                new Literal( "Matte" ),
                TestUri.MATTIAS_PUBLIC_GRAPH );
        
        addStatements( mattiasTypePerson, mattiasNickPublic );
        CompleteStatement addedNick = store().getStatements(
            new WildcardStatement( mattiasNickPublic ),
            false ).iterator().next();
        String key1 = "testKey1";
        String key2 = "testKey2";
        Literal literal1 = new Literal( "A test value" );
        Uri literal2Uri = new Uri( "http://a-date.com" );
        Literal literal2 = new Literal( "2008-10-10", literal2Uri, "en" );
        assertFalse( addedNick.getMetadata().has( key1 ) );
        assertFalse( addedNick.getMetadata().has( key2 ) );
        addedNick.getMetadata().set( key1, literal1 );
        assertEquals( literal1, addedNick.getMetadata().get( key1 ) );
        assertFalse( addedNick.getMetadata().has( key2 ) );
        addedNick.getMetadata().set( key2, literal2 );
        assertEquals( literal2, addedNick.getMetadata().get( key2 ) );
        assertEquals( literal2.getDatatype(),
            addedNick.getMetadata().get( key2 ).getDatatype() );
        assertEquals( literal2.getLanguage(),
            addedNick.getMetadata().get( key2 ).getLanguage() );
        addedNick.getMetadata().remove( key1 );
        assertFalse( addedNick.getMetadata().has( key1 ) );
        deleteEntireNodeSpace();
    }
}
