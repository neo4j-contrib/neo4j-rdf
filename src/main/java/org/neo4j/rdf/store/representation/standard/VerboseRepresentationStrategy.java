package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;

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
public class VerboseRepresentationStrategy
    extends StandardAbstractRepresentationStrategy
{
    /**
     * @param neo the {@link NeoService}.
     */
    public VerboseRepresentationStrategy( RepresentationExecutor executor,
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

        if ( isObjectType( statement.getObject() ) )
        {
            representation = getFourNodeObjectFragment( statement );
        }
        else
        {
            representation = getThreeNodeLiteralFragment( statement );
        }
        return representation;
    }

    private AbstractRepresentation getFourNodeObjectFragment(
        Statement statement )
    {
        AbstractRepresentation representation = newRepresentation();
        AbstractNode subjectNode = getSubjectNode( statement );
        AbstractNode objectNode = getObjectNode( statement );
        String predicate = asUri( statement.getPredicate() );
        AbstractNode predicateNode = getPredicateNode( statement );
        AbstractNode connectorNode = new AbstractNode( null );
        AbstractRelationship subjectToConnectorRel = new AbstractRelationship(
            subjectNode, predicate, connectorNode );
        AbstractRelationship connectorToObjectRel = new AbstractRelationship(
            connectorNode, predicate, objectNode );
        AbstractRelationship connectorToPredicate = new AbstractRelationship(
            connectorNode, RelTypes.CONNECTOR_HAS_PREDICATE.name(),
            predicateNode );

        addSingleContextsToElement( statement, connectorNode );

        representation.addNode( connectorNode );
        representation.addRelationship( subjectToConnectorRel );
        representation.addRelationship( connectorToObjectRel );
        representation.addRelationship( connectorToPredicate );
        return representation;
    }

    private AbstractRepresentation getThreeNodeLiteralFragment(
        Statement statement )
    {
        AbstractRepresentation representation = newRepresentation();
        AbstractNode subjectNode = getSubjectNode( statement );
        String predicate = asUri( statement.getPredicate() );
        AbstractNode predicateNode = getPredicateNode( statement );
        AbstractNode connectorNode = new AbstractNode( null );
        AbstractRelationship subjectToConnectorRel = new AbstractRelationship(
            subjectNode, predicate, connectorNode );
        AbstractRelationship connectorToPredicate = new AbstractRelationship(
            connectorNode, RelTypes.CONNECTOR_HAS_PREDICATE.name(),
            predicateNode );

        addPropertyWithContexts( statement, connectorNode );
        representation.addNode( connectorNode );
        representation.addRelationship( subjectToConnectorRel );
        representation.addRelationship( connectorToPredicate );
        return representation;
    }

    /**
     * Some relationship types used in the representation.
     */
    public static enum RelTypes implements RelationshipType
    {
        /**
         * The connector node --> the predicate node.
         */
        CONNECTOR_HAS_PREDICATE,
    }
}
