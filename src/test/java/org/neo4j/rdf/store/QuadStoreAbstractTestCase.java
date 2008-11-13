package org.neo4j.rdf.store;

import org.neo4j.rdf.model.Wildcard;

public abstract class QuadStoreAbstractTestCase extends StoreTestCase
{
    public static final Wildcard WILDCARD_CONTEXT = new Wildcard( "context" );

    @Override
    protected RdfStore instantiateStore()
    {
        return new VerboseQuadStore( neo(), indexService(), null,
            fulltextIndex() );
    }
}
