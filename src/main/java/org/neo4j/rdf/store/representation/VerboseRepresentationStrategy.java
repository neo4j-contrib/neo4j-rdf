package org.neo4j.rdf.store.representation;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.UriAsrExecutor;

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
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
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
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
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
    
    private void addContextsToRelationship( Statement statement,
        AbstractRelationship relationship )
    {
        for ( Context context : statement.getContexts() )
        {
            relationship.addProperty( CONTEXT_PROPERTY_POSTFIX,
                context.getUriAsString() );
        }
        Map<String, String> contextKeys = new HashMap<String, String>();
        contextKeys.put( CONTEXT_PROPERTY_POSTFIX, null );
        relationship.addLookupInfo( UriAsrExecutor.LOOKUP_CONTEXT_KEYS,
            contextKeys );
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
