package org.neo4j.rdf.store;

public abstract class TripleStoreAbstractTestCase extends StoreTestCase
{
    @Override
    protected RdfStore instantiateStore()
    {
        return new DenseTripleStore( neo(), indexService() );
    }
}
