package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;

public class TestVerboseQuadStore extends QuadStoreAbstractTestCase
{
    @Override
    protected RdfStore instantiateStore()
    {
        return new VerboseQuadStore( neo(), indexService(), null );
    }

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
                new Literal( "Mattias",
            new Uri( "http://www.w3.org/2001/XMLSchema#string" ), "sv" ),
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasNamePrivate =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                new Literal( "Mattias",
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
                Context.NULL );
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
                "Mattias",
                TestUri.MATTIAS_PUBLIC_GRAPH );

        addStatements( mattiasTypePerson, mattiasNamePublic );
        restartTx();

        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                new Wildcard( "p" ),
                new Literal( "Mattias" ),
                new Wildcard( "g" ) ),

            mattiasNamePublic );
        deleteEntireNodeSpace();
    }
}
