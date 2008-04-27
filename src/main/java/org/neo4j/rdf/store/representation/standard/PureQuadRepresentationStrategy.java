package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.NeoService;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.util.index.IndexService;

public class PureQuadRepresentationStrategy
    extends StandardAbstractRepresentationStrategy
{
    public PureQuadRepresentationStrategy( NeoService neo, IndexService index,
        MetaStructure meta )
    {
        super( new PureQuadRepresentationExecutor( neo, index, meta ), meta );
    }

    @Override
    public AbstractRepresentation getAbstractRepresentation(
        Statement statement )
    {
        AbstractRepresentation representation =
            super.getAbstractRepresentation( statement );
        if ( representation != null )
        {
            return representation;
        }

        if ( statement.getPredicate() instanceof Wildcard )
        {
            throw new RuntimeException( "We don't (yet?) support " +
                "wildcard predicates" );
        }

        if ( isObjectType( statement.getObject() ) ||
                pointsToObjectType( ( Uri ) statement.getPredicate() ) )
        {
            // ( S ) -- predicate_uri --> ( O )
            representation = getTwoNodeObjectTypeFragment( statement );
        }
        else
        {
            // ( S ) -- predicate_uri --> ( L )
            representation = getTwoNodeDataTypeFragment( statement );
        }
        return representation;
    }
}
