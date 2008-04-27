package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Wildcard;

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
        CompleteStatement mattiasKnowsEmilPrivate =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_KNOWS,
                TestUri.EMIL,
                TestUri.MATTIAS_PRIVATE_GRAPH );

        addStatements(
            mattiasKnowsEmilPublic,
            mattiasKnowsEmilPrivate );

        assertResult(
            wildcardStatement(
                TestUri.MATTIAS.toUri(),
                TestUri.FOAF_KNOWS.toUri(),
                new Wildcard( "who" ),
                new Wildcard( "g" ) ),

            mattiasKnowsEmilPublic,
            mattiasKnowsEmilPrivate );

        assertResult(
            wildcardStatement(
                TestUri.MATTIAS.toUri(),
                TestUri.FOAF_KNOWS.toUri(),
                TestUri.EMIL.toUri(),
                new Wildcard( "g" ) ),

            mattiasKnowsEmilPublic,
            mattiasKnowsEmilPrivate );

        assertResult(
            wildcardStatement(
                TestUri.MATTIAS.toUri(),
                TestUri.FOAF_KNOWS.toUri(),
                new Wildcard( "who" ),
         new Context( TestUri.MATTIAS_PUBLIC_GRAPH.toUri().getUriAsString() ) ),

            mattiasKnowsEmilPublic );

        assertResult(
            wildcardStatement(
                TestUri.MATTIAS.toUri(),
                TestUri.FOAF_KNOWS.toUri(),
                TestUri.EMIL.toUri(),
        new Context( TestUri.MATTIAS_PRIVATE_GRAPH.toUri().getUriAsString() ) ),

            mattiasKnowsEmilPrivate );
    }
}
