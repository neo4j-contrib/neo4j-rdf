package org.neo4j.rdf.store.representation.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.util.index.IndexService;

public class VerboseQuadExecutor extends UriBasedExecutor
{
    public static final String LITERAL_DATATYPE_KEY =
        START_OF_ILLEGAL_URI + "datatype";
    public static final String LITERAL_LANGUAGE_KEY =
        START_OF_ILLEGAL_URI + "language";
    
    public static enum RelTypes implements RelationshipType
    {
    	REF_CONTEXTS,
    	IS_A_CONTEXT,
    }

    public VerboseQuadExecutor( NeoService neo, IndexService index,
        MetaStructure meta )
    {
        super( neo, index, meta );
    }
    
    public Node getContextsReferenceNode()
    {
    	return this.neoUtil().getOrCreateSubReferenceNode(
    		RelTypes.REF_CONTEXTS );
    }

    @Override
    public void addToNodeSpace( AbstractRepresentation representation )
    {
        if ( isLiteralRepresentation( representation ) )
        {
            handleAddLiteralRepresentation( representation );
        }
        else if ( isObjectTypeRepresentation( representation ) )
        {
            handleAddObjectRepresentation( representation );
        }
        else
        {
            super.addToNodeSpace( representation );
        }
    }

    private void handleAddLiteralRepresentation(
        AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        AbstractNode abstractSubjectNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_SUBJECT );
        AbstractNode abstractMiddleNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_MIDDLE );
        Node subjectNode = lookupOrCreateNode( abstractSubjectNode,
            nodeMapping );
        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_SUBJECT,
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToLiteral = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_MIDDLE,
            VerboseQuadStrategy.TYPE_LITERAL );
        AbstractNode abstractLiteralNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_LITERAL );

        Node[] nodes = findMiddleAndObjectNode( subjectNode, subjectToMiddle,
            middleToLiteral, abstractLiteralNode );
        Node middleNode = nodes[ 0 ];
        Node literalNode = nodes[ 1 ];

        if ( literalNode == null )
        {
            middleNode = createNode( abstractMiddleNode );
            createRelationship( subjectNode, middleNode, subjectToMiddle );
            literalNode = createLiteralNode( abstractLiteralNode );
            createRelationship( middleNode, literalNode, middleToLiteral );
        }
        ensureContextsAreAdded( representation, middleNode, nodeMapping );
    }

    private Relationship findContextRelationship(
        AbstractRelationship abstractRelationship, Node middleNode,
        boolean allowCreate, Map<AbstractNode, Node> nodeMapping )
    {
        AbstractNode abstractContextNode = abstractRelationship.getEndNode();
        Node contextNode = lookupNode( abstractContextNode );
        if ( contextNode == null && !allowCreate )
        {
            return null;
        }
        
        boolean willCreate = contextNode == null;
        contextNode = lookupOrCreateNode( abstractContextNode, nodeMapping );
        if ( willCreate )
        {
	        Node contextRefNode = getContextsReferenceNode();
	        contextRefNode.createRelationshipTo( contextNode,
	        	RelTypes.IS_A_CONTEXT );
        }
        
        Relationship relationship = findDirectRelationship( middleNode,
            relationshipType(  abstractRelationship.getRelationshipTypeName() ),
            contextNode, Direction.OUTGOING );
        return relationship != null ? relationship :
            createRelationship( middleNode, contextNode, abstractRelationship );
    }

    private void handleAddObjectRepresentation(
        AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        AbstractNode abstractSubjectNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_SUBJECT );
        AbstractNode abstractMiddleNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractNode abstractObjectNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_OBJECT );
        Node subjectNode = lookupOrCreateNode( abstractSubjectNode,
            nodeMapping );
        Node objectNode = lookupOrCreateNode( abstractObjectNode,
            nodeMapping );
        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_SUBJECT,
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToObject = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_MIDDLE,
            VerboseQuadStrategy.TYPE_OBJECT );
        Node middleNode = findMiddleAndObjectNode( subjectNode, subjectToMiddle,
            middleToObject, abstractObjectNode )[ 0 ];

        if ( middleNode == null )
        {
            middleNode = createNode( abstractMiddleNode );
            createRelationship( subjectNode, middleNode, subjectToMiddle );
            createRelationship( middleNode, objectNode, middleToObject );
        }
        ensureContextsAreAdded( representation, middleNode, nodeMapping );
    }

    private Node[] findMiddleAndObjectNode( Node subjectNode,
        AbstractRelationship subjectToMiddle,
        AbstractRelationship middleToObject,
        AbstractNode abstractObjectNode )
    {
        Node middleNode = null;
        Node objectNodeToLookFor = null;
        if ( abstractObjectNode.getUriOrNull() != null )
        {
            objectNodeToLookFor = lookupNode( abstractObjectNode );
            if ( objectNodeToLookFor == null )
            {
                return null;
            }
        }

        Node objectNode = null;
        for ( Relationship relationship : subjectNode.getRelationships(
            relationshipType( subjectToMiddle.getRelationshipTypeName() ),
            Direction.OUTGOING ) )
        {
            Node aMiddleNode = relationship.getEndNode();
            for ( Relationship rel : aMiddleNode.getRelationships(
                relationshipType( middleToObject.getRelationshipTypeName() ),
                Direction.OUTGOING ) )
            {
                Node anObjectNode = rel.getEndNode();
                if ( ( objectNodeToLookFor != null &&
                    anObjectNode.equals( objectNodeToLookFor ) ) ||
                    ( objectNodeToLookFor == null && containsProperties(
                        anObjectNode, abstractObjectNode.properties(),
                        LITERAL_DATATYPE_KEY, LITERAL_LANGUAGE_KEY ) ) )
                {
                    middleNode = aMiddleNode;
                    objectNode = anObjectNode;
                    break;
                }
            }
        }
        return new Node[] { middleNode, objectNode };
    }

    private void ensureContextsAreAdded(
        AbstractRepresentation representation, Node middleNode,
        Map<AbstractNode, Node> nodeMapping )
    {
        for ( AbstractRelationship abstractRelationship :
            getContextRelationships( representation, middleNode ) )
        {
            findContextRelationship( abstractRelationship,
                middleNode, true, nodeMapping );
        }
    }

    private Collection<AbstractRelationship> getContextRelationships(
        AbstractRepresentation representation, Node middleNode )
    {
        Collection<AbstractRelationship> list =
            new ArrayList<AbstractRelationship>();
        for ( AbstractRelationship abstractRelationship :
            representation.relationships() )
        {
            if ( relationshipIsType( abstractRelationship,
                VerboseQuadStrategy.TYPE_MIDDLE,
                VerboseQuadStrategy.TYPE_CONTEXT ) )
            {
                list.add( abstractRelationship );
            }
        }
        return list;
    }

    private boolean isLiteralRepresentation(
        AbstractRepresentation representation )
    {
        return getNodeByType( representation,
            VerboseQuadStrategy.TYPE_LITERAL ) != null;
    }

    private boolean isObjectTypeRepresentation(
        AbstractRepresentation representation )
    {
        return getNodeByType( representation,
            VerboseQuadStrategy.TYPE_OBJECT ) != null;
    }

    private AbstractNode getNodeByType(
        AbstractRepresentation representation, String type )
    {
        for ( AbstractNode node : representation.nodes() )
        {
            if ( nodeIsType( node, type ) )
            {
                return node;
            }
        }
        return null;
    }

    private boolean nodeIsType( AbstractNode node, String type )
    {
        String value = ( String ) node.getExecutorInfo(
            VerboseQuadStrategy.EXECUTOR_INFO_NODE_TYPE );
        return value != null && value.equals( type );
    }

    private boolean relationshipIsType( AbstractRelationship relationship,
        String startType, String endType )
    {
        return nodeIsType( relationship.getStartNode(), startType ) &&
            nodeIsType( relationship.getEndNode(), endType );
    }

    private AbstractRelationship findAbstractRelationship(
        AbstractRepresentation representation, String startingType,
        String endingType )
    {
        for ( AbstractRelationship relationship :
            representation.relationships() )
        {
            if ( relationshipIsType( relationship, startingType, endingType ) )
            {
                return relationship;
            }
        }
        return null;
    }

    @Override
    public void removeFromNodeSpace( AbstractRepresentation representation )
    {
        if ( isLiteralRepresentation( representation ) )
        {
            handleRemoveLiteralRepresentation( representation );
        }
        else if ( isObjectTypeRepresentation( representation ) )
        {
            handleRemoveObjectRepresentation( representation );
        }
        else
        {
            super.addToNodeSpace( representation );
        }
    }

    private void handleRemoveObjectRepresentation(
        AbstractRepresentation representation )
    {
        AbstractNode abstractSubjectNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_SUBJECT );
        Node subjectNode = lookupNode( abstractSubjectNode );
        AbstractNode abstractObjectNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_OBJECT );
        Node objectNode = lookupNode( abstractObjectNode );
        if ( subjectNode == null || objectNode == null )
        {
            return;
        }

        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_SUBJECT,
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToObject = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_MIDDLE,
            VerboseQuadStrategy.TYPE_OBJECT );

        Node middleNode = findMiddleAndObjectNode( subjectNode, subjectToMiddle,
            middleToObject, abstractObjectNode )[ 0 ];
        if ( middleNode == null )
        {
            return;
        }

        removeContextRelationships( representation, middleNode );
        if ( !middleNodeHasContexts( middleNode ) )
        {
            disconnectMiddle( middleNode, middleToObject, objectNode,
                subjectNode, subjectToMiddle );
        }

        deleteNodeIfEmpty( abstractSubjectNode, subjectNode );
        // Special case where the subject and object are the same.
        if ( !subjectNode.equals( objectNode ) &&
            nodeIsEmpty( abstractObjectNode, objectNode, true ) )
        {
            deleteNode( objectNode, abstractObjectNode.getUriOrNull() );
        }
    }

    static Iterable<Relationship> getExistingContextRelationships(
        Node middleNode )
    {
        return middleNode.getRelationships(
            VerboseQuadStrategy.RelTypes.IN_CONTEXT );
    }

    private void removeAllContextRelationships( Node middleNode )
    {
        for ( Relationship relationship :
            getExistingContextRelationships( middleNode ) )
        {
            deleteRelationship( relationship );
        }
    }

    private void removeSelectedContextRelationships( Node middleNode,
        Collection<AbstractRelationship> contextRelationships )
    {
        for ( AbstractRelationship contextRelationship :
            contextRelationships )
        {
            Node contextNode = lookupNode(
                contextRelationship.getEndNode() );
            if ( contextNode != null )
            {
                Relationship relationship = findDirectRelationship( middleNode,
                    relationshipType(
                    contextRelationship.getRelationshipTypeName() ),
                    contextNode, Direction.OUTGOING );
                if ( relationship != null )
                {
                    deleteRelationship( relationship );
                }
            }
        }
    }

    private void removeContextRelationships(
        AbstractRepresentation representation, Node middleNode )
    {
        Collection<AbstractRelationship> contextRelationships =
            getContextRelationships( representation, middleNode );
        if ( contextRelationships.isEmpty() )
        {
            removeAllContextRelationships( middleNode );
        }
        else
        {
            removeSelectedContextRelationships( middleNode,
                contextRelationships );
        }
    }

    private void handleRemoveLiteralRepresentation(
        AbstractRepresentation representation )
    {
        AbstractNode abstractSubjectNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_SUBJECT );
        Node subjectNode = lookupNode( abstractSubjectNode );
        if ( subjectNode == null )
        {
            return;
        }

        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_SUBJECT,
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToLiteral = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_MIDDLE,
            VerboseQuadStrategy.TYPE_LITERAL );
        AbstractNode abstractLiteralNode = getNodeByType( representation,
            VerboseQuadStrategy.TYPE_LITERAL );

        Node[] nodes = findMiddleAndObjectNode( subjectNode, subjectToMiddle,
            middleToLiteral, abstractLiteralNode );
        Node middleNode = nodes[ 0 ];
        Node literalNode = nodes[ 1 ];
        if ( literalNode == null )
        {
            return;
        }

        removeContextRelationships( representation, middleNode );
        if ( !middleNodeHasContexts( middleNode ) )
        {
            deleteMiddleAndLiteral( middleNode, middleToLiteral,
                literalNode, subjectNode, subjectToMiddle );
        }
        deleteNodeIfEmpty( abstractSubjectNode, subjectNode );
    }

    private boolean middleNodeHasContexts( Node middleNode )
    {
        return getExistingContextRelationships(
            middleNode ).iterator().hasNext();
    }

    private String guessPredicateKey( Iterable<String> keys )
    {
        for ( String key : keys )
        {
            if ( !key.equals( LITERAL_DATATYPE_KEY ) &&
                !key.equals( LITERAL_LANGUAGE_KEY ) )
            {
                return key;
            }
        }
        return null;
    }

    private void deleteMiddleAndLiteral( Node middleNode,
        AbstractRelationship middleToLiteral, Node literalNode,
        Node subjectNode, AbstractRelationship subjectToMiddle )
    {
        disconnectMiddle( middleNode, middleToLiteral, literalNode,
            subjectNode, subjectToMiddle );
        String predicate = guessPredicateKey( literalNode.getPropertyKeys() );
        Object value = literalNode.getProperty( predicate );
        deleteLiteralNode( literalNode, predicate, value );
    }

    private void disconnectMiddle( Node middleNode,
        AbstractRelationship middleToOther, Node otherNode,
        Node subjectNode, AbstractRelationship subjectToMiddle )
    {
        ensureDirectlyDisconnected( middleNode, middleToOther, otherNode );
        ensureDirectlyDisconnected( subjectNode, subjectToMiddle, middleNode );
        deleteNode( middleNode, null );
    }
}
