package org.neo4j.rdf.store;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.index.lucene.LuceneIndexService;

public class CachingLuceneIndexService extends LuceneIndexService
{
    public CachingLuceneIndexService( GraphDatabaseService neo )
    {
        super( neo );
        enableCache( AbstractUriBasedExecutor.URI_PROPERTY_KEY,
            100000 );
    }
}
