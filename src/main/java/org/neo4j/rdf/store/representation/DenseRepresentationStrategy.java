package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.NeoService;
import org.neo4j.rdf.model.Statement;

/**
 * S/P/O represented as:
 * if object property: ( S ) -- predicate_uri_as_reltype --> ( O )
 * if data property: ( S ) with property [key=predicate_uri, value=O]
 */
public class DenseRepresentationStrategy extends IndexRepresentationStrategy
{
	public DenseRepresentationStrategy( NeoService neo )
    {
        super( neo );
    }

    public AbstractStatementRepresentation getAbstractRepresentation(
        Statement statement )
    {
        if ( statement.getObject().isObjectProperty() )
        {
            // ( S ) -- predicate_uri --> ( O )
            return createTwoNodeFragment( statement );
        }
        else
        {
            // ( S ) with property [key=predicate_uri, value=O]
            return createOneNodeFragment( statement );
        }
    }

    private AbstractStatementRepresentation createTwoNodeFragment(
        Statement statement )
    {
        AbstractStatementRepresentation representation =
            new AbstractStatementRepresentation();
        AbstractNode subjectNode = getSubjectNode( statement );
        AbstractNode objectNode = getObjectNode( statement );
        representation.addNode( subjectNode );
        representation.addNode( objectNode );
        representation.addRelationship( new AbstractRelationship( subjectNode,
            statement.getPredicate().uriAsString(), objectNode ) );
        return representation;
    }
}
