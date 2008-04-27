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
        CompleteStatement mattiasNamePublic =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_NICK,
                "Mattias",
                TestUri.MATTIAS_PUBLIC_GRAPH );
        CompleteStatement mattiasKnowsEmilPrivate =
            completeStatement(
                TestUri.MATTIAS,
                TestUri.FOAF_KNOWS,
                TestUri.EMIL,
                TestUri.MATTIAS_PRIVATE_GRAPH );

        addStatements(
            mattiasKnowsEmilPublic,
            mattiasKnowsEmilPrivate,
            mattiasNamePublic );

        assertResult(
            wildcardStatement(
                new Wildcard( "s" ),
                new Wildcard( "p" ),
                new Wildcard( "o" ),
                new Context( TestUri.MATTIAS_PUBLIC_GRAPH.toUri().getUriAsString() ) ),

            mattiasKnowsEmilPublic,
            mattiasNamePublic );
    }
}
