package org.neo4j.rdf.store;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.rdf.fulltext.SimpleFulltextIndex;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;

public class TestBasicQuadContract extends QuadStoreAbstractTestCase
{
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
        return new VerboseQuadStore( graphDb(), indexService(), null,
            new SimpleFulltextIndex( graphDb(), new File( getBasePath(),
                "fulltext" ) ) );
    }

    @Before
    public void addInitialStatements()
    {
        addStatements(
            EMIL_KNOWS_MATTIAS_PUBLIC,
            EMIL_KNOWS_MATTIAS_PRIVATE,
            EMIL_KNOWS_MATTIAS_NULL );
    }

    @After
    public void clearAllStatements()
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


    // Test getStatements()

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testGetSPOC1C2()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PUBLIC_GRAPH ),
            EMIL_KNOWS_MATTIAS_PUBLIC );
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PRIVATE_GRAPH ),
            EMIL_KNOWS_MATTIAS_PRIVATE );
    }

    // Test removeStatements()

    @Test
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

    @Test
    public void testRemoveSPONull()
    {
        store().removeStatements(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                Context.NULL ) );
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                WILDCARD_CONTEXT ),
            EMIL_KNOWS_MATTIAS_PUBLIC,
            EMIL_KNOWS_MATTIAS_PRIVATE );
    }

    @Test
    public void testRemoveSPOC()
    {
        store().removeStatements(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PUBLIC_GRAPH ) );
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                WILDCARD_CONTEXT ),
            EMIL_KNOWS_MATTIAS_PRIVATE,
            EMIL_KNOWS_MATTIAS_NULL );
    }

    @Test
    public void testRemoveSPOC1C2()
    {
        store().removeStatements(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PUBLIC_GRAPH ) );
        store().removeStatements(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PRIVATE_GRAPH ) );
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                WILDCARD_CONTEXT ),
            EMIL_KNOWS_MATTIAS_NULL );
    }

    // Test addStatements()

    @Test
    public void testAddSPONull()
    {
        clearAllStatements();
        store().addStatements(
            completeStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                Context.NULL ) );
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                WILDCARD_CONTEXT ),
            EMIL_KNOWS_MATTIAS_NULL );
    }
}
