package org.neo4j.rdf.store;

public class TestBasicQuadContract extends QuadStoreAbstractTestCase
{
    @Override
    protected RdfStore instantiateStore()
    {
        return new VerboseQuadStore( neo(), indexService(), null );
    }
    
    public void testGetSPONull()
    {
        addStatements(
            completeStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.EMIL_PUBLIC_GRAPH ),
            completeStatement(
                TestUri.EMIL,
                TestUri.FOAF_KNOWS,
                TestUri.MATTIAS,
                TestUri.NULL_CONTEXT )
            );
        store().getStatements(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                TestUri.FOAF_KNOWS.toUri(),
                TestUri.MATTIAS.toUri(),
                TestUri.NULL_CONTEXT.toUri() ),
                false );
    }

}
