package org.neo4j.rdf.store.representation.standard;

import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;

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
    
    /**
     * @param neo {@link NeoService}.
     * @param meta {@link MetaStructure}.
     */
    public VerboseRepresentationStrategy( NeoService neo, MetaStructure meta )
    {
        super( neo, meta );
    }
    
    @Override
    protected boolean addToRepresentation(
        AbstractRepresentation representation,
        Map<Value, AbstractNode> nodeMapping, Statement statement )
    {
        if ( !super.addToRepresentation( representation, nodeMapping,
            statement ) )
        {
            if ( isObjectType( statement.getObject() ) )
            {
                addFourNodeFragment( representation, nodeMapping, statement );
            }
            else
            {
                addOneNodeFragment( representation, nodeMapping, statement );
            }
        }
        return true;
    }

    private void addFourNodeFragment(
        AbstractRepresentation representation,
        Map<Value, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode subjectNode = getSubjectNode( nodeMapping, statement );
        AbstractNode objectNode = getObjectNode( nodeMapping, statement );
        String predicate = asUri( statement.getPredicate() );
        AbstractNode predicateNode = getPredicateNode( nodeMapping, statement );
        AbstractNode connectorNode = new AbstractNode( null );
        AbstractRelationship subjectToConnectorRel = new AbstractRelationship(
            subjectNode, predicate, connectorNode );
        AbstractRelationship connectorToObjectRel = new AbstractRelationship(
            connectorNode, predicate, objectNode );
        AbstractRelationship connectorToPredicate = new AbstractRelationship(
            connectorNode, RelTypes.CONNECTOR_HAS_PREDICATE.name(),
            predicateNode );
        
        addContextsToRelationship( statement, subjectToConnectorRel );
        addContextsToRelationship( statement, connectorToObjectRel );
        addContextsToRelationship( statement, connectorToPredicate );

        representation.addNode( connectorNode );
        representation.addRelationship( subjectToConnectorRel );
        representation.addRelationship( connectorToObjectRel );
        representation.addRelationship( connectorToPredicate );
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
