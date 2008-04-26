package org.neo4j.rdf.store.representation.standard;

import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.util.index.Index;

public class PureQuadRepresentationStrategy
    extends StandardAbstractRepresentationStrategy
{
    public PureQuadRepresentationStrategy( NeoService neo, Index index,
        MetaStructure meta )
    {
        super( new PureQuadRepresentationExecutor( neo, index, meta ), meta );
    }

    @Override
    protected boolean addToRepresentation(
        AbstractRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        if ( !super.addToRepresentation(
            representation, nodeMapping, statement ) )
        {
            if ( statement.getPredicate() instanceof Wildcard )
            {
                throw new RuntimeException( "We don't (yet?) support " +
                    "wildcard predicates" );
            }

            if ( isObjectType( statement.getObject() ) ||
                    pointsToObjectType( ( Uri ) statement.getPredicate() ) )
            {
                // ( S ) -- predicate_uri --> ( O )
                addTwoNodeObjectTypeFragment(
                    representation, nodeMapping, statement );
            }
            else
            {
                // ( S ) -- predicate_uri --> ( L )
                addTwoNodeDataTypeFragment( representation, nodeMapping,
                    statement );
            }
        }
        return true;
    }
}
