package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.rdf.model.Statement;

/**
 * Uses a more verbose representation of statements, like this:
 * 
 *                  (P)
 *                   ^
 *                   |
 *                   |
 *    (S) --------> ( ) --------> (O)
 *    
 */
public class VerboseRepresentationStrategy extends IndexRepresentationStrategy
{
    /**
     * @param neo the {@link NeoService}.
     */
    public VerboseRepresentationStrategy( NeoService neo )
    {
        super( neo );
    }

    public AbstractStatementRepresentation getAbstractRepresentation(
        Statement statement )
    {
        if ( statement.getObject().isObjectProperty() )
        {
            return createFourNodeFragment( statement );
        }
        else
        {
            return createOneNodeFragment( statement );
        }
    }

    private AbstractStatementRepresentation createFourNodeFragment(
        Statement statement )
    {
        AbstractStatementRepresentation representation =
            new AbstractStatementRepresentation();
        AbstractNode subjectNode = getSubjectNode( statement );
        AbstractNode objectNode = getObjectNode( statement );
        String predicate = statement.getPredicate().uriAsString();
        AbstractNode predicateNode = new AbstractNode( predicate );
        AbstractNode connectorNode = new AbstractNode( null );
        AbstractRelationship subjectToConnectorRel = new AbstractRelationship(
            subjectNode, predicate, connectorNode );
        AbstractRelationship connectorToObjectRel = new AbstractRelationship(
            connectorNode, predicate, objectNode );
        AbstractRelationship connectorToPredicate = new AbstractRelationship(
            connectorNode, RelTypes.CONNECTOR_HAS_PREDICATE.name(),
            predicateNode );

        representation.addNode( subjectNode );
        representation.addNode( objectNode );
        representation.addNode( predicateNode );
        representation.addNode( connectorNode );
        representation.addRelationship( subjectToConnectorRel );
        representation.addRelationship( connectorToObjectRel );
        representation.addRelationship( connectorToPredicate );
        return representation;
    }

    private static enum RelTypes implements RelationshipType
    {
        /**
         * The connector node --> the predicate node.
         */
        CONNECTOR_HAS_PREDICATE,
    }
}
