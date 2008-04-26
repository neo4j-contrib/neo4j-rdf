package org.neo4j.rdf.store.representation.standard;

import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.AbstractNode;
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
                addTwoNodeObjectTypeFragment( representation,
                    nodeMapping, statement );
            }
            else
            {
                // ( S ) with property [key=predicate_uri, value=O]
                addOneNodeWithLiteralsAsProperties( representation, nodeMapping,
                    statement );
            }
	    }
        return true;
    }
}
