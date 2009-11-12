package org.neo4j.rdf.store.representation.standard;

import org.neo4j.meta.model.MetaModel;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;

/**
 * S/P/O represented as:
 * if object property: ( S ) -- predicate_uri_as_reltype --> ( O )
 * if data property: ( S ) with property [key=predicate_uri, value=O]
 */
public class DenseTripleStrategy
    extends StandardAbstractRepresentationStrategy
{
	public DenseTripleStrategy( RepresentationExecutor executor,
	    MetaModel meta )
    {
	    super( executor, meta );
    }
	
    @Override
    public UriBasedExecutor getExecutor()
    {
        return ( UriBasedExecutor ) super.getExecutor();
    }

    @Override
    public AbstractRepresentation getAbstractRepresentation(
        Statement statement, AbstractRepresentation representation )
    {
        if ( super.getAbstractRepresentation( statement, representation ) !=
            null )
        {
            return representation;
        }

        if ( statement.getPredicate() instanceof Wildcard )
        {
            throw new RuntimeException( "We don't (yet?) support " +
                "wildcard predicates" );
        }

        if ( objectIsObjectType( statement ) )
        {
            // ( S ) -- predicate_uri --> ( O )
            getTwoNodeObjectTypeFragment( statement, representation );
        }
        else
        {
            // ( S ) with property [key=predicate_uri, value=O]
            getOneNodeWithLiteralsAsProperties( statement, representation );
        }
        return representation;
    }
}
