package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.NeoService;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;

/**
 * S/P/O represented as:
 * if object property: ( S ) -- predicate_uri_as_reltype --> ( O )
 * if data property: ( S ) with property [key=predicate_uri, value=O]
 */
public class DenseRepresentationStrategy
    extends StandardAbstractRepresentationStrategy
{
    /**
     * @param neo the {@link NeoService}.
     */
	public DenseRepresentationStrategy( RepresentationExecutor executor,
	    MetaStructure meta )
    {
	    super( executor, meta );
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
            // ( S ) with property [key=predicate_uri, value=O]
            representation = getOneNodeWithLiteralsAsProperties( statement );
        }
        return representation;
    }
}
