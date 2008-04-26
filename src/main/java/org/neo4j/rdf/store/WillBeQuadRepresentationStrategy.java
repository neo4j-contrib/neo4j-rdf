package org.neo4j.rdf.store;

import org.neo4j.api.core.NeoService;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.standard.DenseRepresentationStrategy;

public class WillBeQuadRepresentationStrategy
    extends DenseRepresentationStrategy
{
    public WillBeQuadRepresentationStrategy( NeoService neo,
        MetaStructure meta )
    {
        super( neo, meta );
    }
    
    @Override
    protected boolean pointsToObjectType( Uri predicate )
    {
        return super.pointsToObjectType( predicate );
    }
}
