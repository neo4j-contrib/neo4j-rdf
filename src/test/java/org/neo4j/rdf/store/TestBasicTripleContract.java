package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;

public class TestBasicTripleContract extends TripleStoreAbstractTestCase
{
    private static final CompleteStatement EMIL_KNOWS_MATTIAS =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_KNOWS,
            TestUri.MATTIAS,
            Context.NULL );

    private static final CompleteStatement EMIL_KNOWS_JOHAN =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_KNOWS,
            TestUri.JOHAN,
            Context.NULL );
    
    private static final CompleteStatement MATTIAS_KNOWS_JOHAN =
        completeStatement(
            TestUri.MATTIAS,
            TestUri.FOAF_KNOWS,
            TestUri.JOHAN,
            Context.NULL );
    
    private static final CompleteStatement EMIL_TYPE_PERSON =
        completeStatement(
            TestUri.EMIL,
            TestUri.RDF_TYPE,
            TestUri.PERSON,
            Context.NULL );

    private static final CompleteStatement EMIL_NAME =
        completeStatement(
            TestUri.EMIL,
            TestUri.FOAF_NAME,
            new Literal( "Emil" ),
            Context.NULL );

    private static final CompleteStatement MATTIAS_NAME =
        completeStatement(
            TestUri.MATTIAS,
            TestUri.FOAF_NAME,
            new Literal( "Mattias" ),
            Context.NULL );
    
    @Override
    protected RdfStore instantiateStore()
    {
        return new DenseTripleStore( graphDb(), indexService(), null, null );
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        addInitialStatements();
    }

    @Override
    protected void tearDown() throws Exception
    {
        clearAllStatements();
        super.tearDown();
    }

    private void addInitialStatements()
    {
        addStatements( getTestStatements() );
    }
    
    private CompleteStatement[] getTestStatements()
    {
        return new CompleteStatement[] {
            EMIL_KNOWS_MATTIAS,
            EMIL_KNOWS_JOHAN,
            EMIL_TYPE_PERSON,
            EMIL_NAME,
            MATTIAS_NAME,
            MATTIAS_KNOWS_JOHAN,
        };
    }

    private void clearAllStatements()
    {
        for ( CompleteStatement statement : getTestStatements() )
        {
            WildcardStatement wildcardStatement =
                statement.asWildcardStatement();
            removeStatements( wildcardStatement );
            assertResultCount( wildcardStatement, 0 );
        }
    }

    public void testGetSPO()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                Context.NULL ),
            EMIL_KNOWS_MATTIAS );
        assertResult(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                TestUri.FOAF_NAME.toUri(),
                EMIL_NAME.getObject(),
                Context.NULL ),
            EMIL_NAME );
    }

    public void testGetSP_()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                TestUri.FOAF_KNOWS.toUri(),
                new Wildcard( "o" ),
                Context.NULL ),
            EMIL_KNOWS_MATTIAS, EMIL_KNOWS_JOHAN );
        assertResult(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                TestUri.FOAF_NAME.toUri(),
                new Wildcard( "o" ),
                Context.NULL ),
            EMIL_NAME );
    }

    public void testGetS__()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                new Wildcard( "p" ),
                new Wildcard( "o" ),
                Context.NULL ),
            EMIL_KNOWS_MATTIAS, EMIL_KNOWS_JOHAN, EMIL_TYPE_PERSON, EMIL_NAME );
        assertResult(
            wildcardStatement(
                TestUri.MATTIAS.toUri(),
                new Wildcard( "p" ),
                new Wildcard( "o" ),
                Context.NULL ),
            MATTIAS_NAME, MATTIAS_KNOWS_JOHAN );
    }

    public void testGetS_O()
    {
        assertResult(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                new Wildcard( "p" ),
                TestUri.MATTIAS.toUri(),
                Context.NULL ),
            EMIL_KNOWS_MATTIAS );
        assertResult(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                new Wildcard( "p" ),
                EMIL_NAME.getObject(),
                Context.NULL ),
            EMIL_NAME );
    }
    
    public void testGet_PO()
    {
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                TestUri.FOAF_KNOWS.toUri(),
                TestUri.MATTIAS.toUri(),
                Context.NULL ),
            EMIL_KNOWS_MATTIAS );
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                TestUri.FOAF_NAME.toUri(),
                EMIL_NAME.getObject(),
                Context.NULL ),
            EMIL_NAME );
    }

    public void testGet__O()
    {
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                new Wildcard( "p" ),
                TestUri.JOHAN.toUri(),
                Context.NULL ),
            EMIL_KNOWS_JOHAN, MATTIAS_KNOWS_JOHAN );
        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                new Wildcard( "p" ),
                EMIL_NAME.getObject(),
                Context.NULL ),
            EMIL_NAME );
    }

    // TODO Not implemented in the triple store yet
//    public void testGet_P_()
//    {
//        assertResult(
//            wildcardStatement(
//                new Wildcard( "s" ),
//                TestUri.FOAF_KNOWS.toUri(),
//                new Wildcard( "o" ),
//                Context.NULL ),
//            EMIL_KNOWS_JOHAN, MATTIAS_KNOWS_JOHAN, EMIL_KNOWS_MATTIAS );
//        assertResult(
//            wildcardStatement(
//                new Wildcard( "s" ),
//                TestUri.FOAF_NAME.toUri(),
//                new Wildcard( "o" ),
//                Context.NULL ),
//            EMIL_NAME, MATTIAS_NAME );
//    }
//    
//    public void testGet___()
//    {
//        assertResult(
//            wildcardStatement(
//                new Wildcard( "s" ),
//                new Wildcard( "p" ),
//                new Wildcard( "o" ),
//                Context.NULL ),
//            getTestStatements() );
//    }
}
