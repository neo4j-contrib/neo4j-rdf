package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Wildcard;

public class TestBasicQuadContract extends QuadStoreAbstractTestCase
{
    private static final Wildcard WILDCARD_SUBJECT = new Wildcard( "subject" );
    private static final Wildcard WILDCARD_PREDICATE= new Wildcard(
        "predicate" );
    private static final Wildcard WILDCARD_OBJECT = new Wildcard( "object" );
    private static final Wildcard WILDCARD_CONTEXT = new Wildcard( "context" );
    
    private static final CompleteStatement EMIL_KNOWS_MATTIAS_PUBLIC =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_KNOWS,
            TestUri.MATTIAS,
            TestUri.EMIL_PUBLIC_GRAPH );

    private static final CompleteStatement EMIL_KNOWS_MATTIAS_PRIVATE =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_KNOWS,
            TestUri.MATTIAS,
            TestUri.EMIL_PRIVATE_GRAPH );
    
    private static final CompleteStatement EMIL_KNOWS_MATTIAS_NULL =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_KNOWS,
            TestUri.MATTIAS,
            Context.NULL );

    @Override
    protected RdfStore instantiateStore()
    {
        return new VerboseQuadStore( neo(), indexService(), null );
    }
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        initializeStore();
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        deleteEntireNodeSpace();
        super.tearDown();
    }
    
    private void initializeStore()
    {
        addStatements( EMIL_KNOWS_MATTIAS_PUBLIC, EMIL_KNOWS_MATTIAS_PRIVATE,
            EMIL_KNOWS_MATTIAS_NULL ); 
    }

    // Test getStatements()
    
    public void testGetSPO()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                WILDCARD_CONTEXT ),
            EMIL_KNOWS_MATTIAS_PUBLIC,
            EMIL_KNOWS_MATTIAS_PRIVATE,
            EMIL_KNOWS_MATTIAS_NULL );
    }
    
    public void testGetSPONull()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                Context.NULL ),
            EMIL_KNOWS_MATTIAS_NULL );
    }
    
    public void testGetSPOC()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PUBLIC_GRAPH ),
            EMIL_KNOWS_MATTIAS_PUBLIC );        
    }

    public void testGetSPOC1C2()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PUBLIC_GRAPH ),
            EMIL_KNOWS_MATTIAS_NULL );        
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PRIVATE_GRAPH ),
            EMIL_KNOWS_MATTIAS_PRIVATE );        
    }
    
    // Test removeStatements()

    public void testRemoveSPO()
    {
        store().removeStatements(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                WILDCARD_CONTEXT ) );
        assertResultCount(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                WILDCARD_CONTEXT ), 0 );
    }
//    
//    private void assertResultCount( WildcardStatement wildcard, int expected )
//    {
//        
//    }
}
