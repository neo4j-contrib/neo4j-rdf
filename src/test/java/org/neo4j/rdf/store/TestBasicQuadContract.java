package org.neo4j.rdf.store;

import java.util.Iterator;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Wildcard;

public class TestBasicQuadContract extends QuadStoreAbstractTestCase
{
    private static final boolean PRINT_STUFF = true;

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
//            completeStatement(
//                TestUri.EMIL,
//                TestUri.FOAF_KNOWS,
//                TestUri.MATTIAS,
//                TestUri.EMIL_PUBLIC_GRAPH ),
//            completeStatement(
//                TestUri.EMIL,
//                TestUri.FOAF_KNOWS,
//                TestUri.MATTIAS,
//                TestUri.EMIL_PRIVATE_GRAPH ),
//            completeStatement(
//                TestUri.EMIL,
//                TestUri.FOAF_KNOWS,
//                TestUri.MATTIAS,
//                Context.NULL )
//            );        
    }

    public void testGetSPONull()
    {
        Iterator<CompleteStatement> results = store().getStatements(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                Context.NULL ),
                false ).iterator();
        
        CompleteStatement result = results.next();
        assertFalse( results.hasNext() );
        assertEquivalentStatement( result, EMIL_KNOWS_MATTIAS_NULL );        
    }
    
    private void assertEquivalentStatement( Statement first, Statement second )
    {
        assertEquals( first.getSubject(), second.getSubject() );
        assertEquals( first.getPredicate(), second.getPredicate() );
        assertEquals( first.getObject(), second.getObject() );
        assertEquals( first.getContext(), second.getContext() );
        if ( PRINT_STUFF )
        {
            debug( first.toString() );
        }
    }

}
