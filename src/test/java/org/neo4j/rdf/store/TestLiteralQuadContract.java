package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Literal;

public class TestLiteralQuadContract extends QuadStoreAbstractTestCase
{
    private static final CompleteStatement EMIL_NICK_EMIL_PUBLIC =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_NICK,
            new Literal( "Emil" ),
            TestUri.EMIL_PUBLIC_GRAPH );

    private static final CompleteStatement EMIL_NICK_EMPA_PUBLIC =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_NICK,
            new Literal( "Empa" ),
            TestUri.EMIL_PUBLIC_GRAPH );

    private static final CompleteStatement EMIL_NICK_EMPA_PRIVATE =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_NICK,
            new Literal( "Empa" ),
            TestUri.EMIL_PRIVATE_GRAPH );

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        addInitialStatements();
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        deleteEntireNodeSpace();
        super.tearDown();
    }

    private void addInitialStatements()
    {
        addStatements(
            EMIL_NICK_EMIL_PUBLIC,
            EMIL_NICK_EMPA_PRIVATE,
            EMIL_NICK_EMPA_PUBLIC );
    }

    public void testGetNick()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                TestUri.FOAF_NICK.toUri(),
                WILDCARD_OBJECT,
                WILDCARD_CONTEXT ),
            EMIL_NICK_EMIL_PUBLIC,
            EMIL_NICK_EMPA_PRIVATE,
            EMIL_NICK_EMPA_PUBLIC );
    }
}
