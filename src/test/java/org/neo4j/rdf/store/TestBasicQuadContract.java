package org.neo4j.rdf.store;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;

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
                Context.NULL )
            );
        
        Iterable<CompleteStatement> results = store().getStatements(
            wildcardStatement(
                TestUri.EMIL.toUri(),
                TestUri.FOAF_KNOWS.toUri(),
                TestUri.MATTIAS.toUri(),
                Context.NULL ),
                false );
        
        for ( CompleteStatement result : results )
        {
            System.out.println( result );
        }        
        
    }

}
