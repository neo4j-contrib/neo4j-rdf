package org.neo4j.rdf.store.testrepresentation;

import org.neo4j.api.core.NeoService;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;

/**
 * S/P/O represented as:<br/>
 * if object property: ( S ) -- predicate_uri_as_reltype --> ( O )
 * if data property: ( S ) with property [key=predicate_uri, value=O]
 */
public class DenseRepresentationStrategy extends IndexRepresentationStrategy
{
	public DenseRepresentationStrategy( NeoService neo )
	{
		super( neo );
	}
	
    public AbstractStatementRepresentation getAbstractRepresentation( Statement
        statement )
    {
        if ( statement.getObject().isObjectProperty() )
        {
            // ( S ) -- predicate_uri --> ( O )
            return createTwoNodesWithRelationship( statement );
        }
        else
        {
            // ( S ) with property [key=predicate_uri, value=O]
            return createSingleNodeWithDataProperty( statement );
        }
    }
    
    private AbstractStatementRepresentation createTwoNodesWithRelationship(
        Statement statement )
    {
        AbstractStatementRepresentation representation =
        	new AbstractStatementRepresentation();
        AbstractNode subjectNode = getSubjectNode( statement );
        AbstractNode objectNode = getObjectNode( statement );
        representation.addNode( subjectNode );
        representation.addNode( objectNode );
        representation.addRelationship( new AbstractRelationship(
        	subjectNode, statement.getPredicate().uriAsString(), objectNode ) );
        return representation;
    }
}
