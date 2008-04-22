package org.neo4j.rdf.store.representation;

import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.rdf.model.Statement;

/**
 * S/P/O represented as:
 * if object property: ( S ) -- predicate_uri_as_reltype --> ( O )
 * if data property: ( S ) with property [key=predicate_uri, value=O]
 */
public class DenseRepresentationStrategy extends IndexRepresentationStrategy
{
    /**
     * @param neo the {@link NeoService}.
     */
	public DenseRepresentationStrategy( NeoService neo )
    {
        super( neo );
    }

	@Override
    protected boolean addToRepresentation(
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
	    if ( !super.addToRepresentation(
	        representation, nodeMapping, statement ) )
	    {
            if ( statement.getObject().isObjectProperty() )
            {
                // ( S ) -- predicate_uri --> ( O )
                addTwoNodeFragment( representation, nodeMapping, statement );
            }
            else
            {
                // ( S ) with property [key=predicate_uri, value=O]
                addOneNodeFragment( representation, nodeMapping, statement );
            }
	    }
        return true;
    }

    private void addTwoNodeFragment(
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode subjectNode = getSubjectNode( nodeMapping, statement );
        AbstractNode objectNode = getObjectNode( nodeMapping, statement );
        representation.addRelationship( new AbstractRelationship( subjectNode,
            statement.getPredicate().uriAsString(), objectNode ) );
    }
}
