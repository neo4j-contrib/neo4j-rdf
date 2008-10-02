package org.neo4j.rdf.store;

import org.neo4j.api.core.NeoService;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.util.index.LuceneIndexService;

public class CachingLuceneIndexService extends LuceneIndexService
{
    public CachingLuceneIndexService( NeoService neo )
    {
        super( neo );
        enableCache( AbstractUriBasedExecutor.URI_PROPERTY_KEY,
            100000 );
    }
}
