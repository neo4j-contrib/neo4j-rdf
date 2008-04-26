package org.neo4j.rdf.store.representation.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.util.index.Index;

public class AlwaysMiddleExecutor extends UriBasedExecutor
{
    public AlwaysMiddleExecutor( NeoService neo, Index index,
        MetaStructure meta )
    {
        super( neo, index, meta );
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
            AlwaysMiddleNodesStrategy.TYPE_SUBJECT );
        AbstractNode abstractMiddleNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_MIDDLE );
        Node subjectNode = lookupOrCreateNode( abstractSubjectNode,
            nodeMapping );
        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_SUBJECT,
            AlwaysMiddleNodesStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToLiteral = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_MIDDLE,
            AlwaysMiddleNodesStrategy.TYPE_LITERAL );
        AbstractNode abstractLiteralNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_LITERAL );

        Node[] nodes = findLiteralNode( subjectNode, subjectToMiddle,
            middleToLiteral, abstractLiteralNode );
        Node middleNode = nodes[ 0 ];
        Node literalNode = nodes[ 1 ];

        if ( literalNode == null )
        {
            middleNode = neo().createNode();
            debug( "\t+Node (middle) " + middleNode );
            Relationship rel = subjectNode.createRelationshipTo( middleNode,
                relationshipType( subjectToMiddle.getRelationshipTypeName() ) );
            debugCreateRelationship( rel );
            applyRepresentation( abstractMiddleNode, middleNode );

            literalNode = neo().createNode();
            debug( "\t+Node (literal) " + literalNode );
            rel = middleNode.createRelationshipTo( literalNode,
                relationshipType( middleToLiteral.getRelationshipTypeName() ) );
            debugCreateRelationship( rel );
            applyRepresentation( abstractLiteralNode, literalNode );
        }
        ensureContextsAreAdded( representation, middleNode, nodeMapping );
    }

    private Node[] findLiteralNode( Node subjectNode,
        AbstractRelationship subjectToMiddle,
        AbstractRelationship middleToLiteral,
        AbstractNode abstractLiteralNode )
    {
        Node middleNode = null;
        Node literalNode = null;
        for ( Relationship relationship : subjectNode.getRelationships(
            relationshipType( subjectToMiddle.getRelationshipTypeName() ),
            Direction.OUTGOING ) )
        {
            Node aMiddleNode = relationship.getEndNode();
            for ( Relationship rel : aMiddleNode.getRelationships(
                relationshipType( middleToLiteral.getRelationshipTypeName() ),
                Direction.OUTGOING ) )
            {
                Node aLiteralNode = rel.getEndNode();
                if ( containsProperties( aLiteralNode,
                    abstractLiteralNode.properties() ) )
                {
                    middleNode = aMiddleNode;
                    literalNode = aLiteralNode;
                    break;
                }
            }
        }
        return new Node[] { middleNode, literalNode };
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
        contextNode = lookupOrCreateNode( abstractContextNode, nodeMapping );
        for ( Relationship relationship : middleNode.getRelationships(
            relationshipType( abstractRelationship.getRelationshipTypeName() ),
            Direction.OUTGOING ) )
        {
            Node aContextNode = relationship.getEndNode();
            if ( aContextNode.equals( contextNode ) )
            {
                return relationship;
            }
        }
        Relationship relationship = middleNode.createRelationshipTo(
            contextNode, relationshipType(
                abstractRelationship.getRelationshipTypeName() ) );
        debugCreateRelationship( relationship );
        return relationship;
    }

    private void handleAddObjectRepresentation(
        AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        AbstractNode abstractSubjectNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_SUBJECT );
        AbstractNode abstractMiddleNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_MIDDLE );
        AbstractNode abstractObjectNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_OBJECT );
        Node subjectNode = lookupOrCreateNode( abstractSubjectNode,
            nodeMapping );
        Node objectNode = lookupOrCreateNode( abstractObjectNode,
            nodeMapping );
        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_SUBJECT,
            AlwaysMiddleNodesStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToObject = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_MIDDLE,
            AlwaysMiddleNodesStrategy.TYPE_OBJECT );
        Node middleNode = findObjectMiddleNode( subjectNode, subjectToMiddle,
            middleToObject, objectNode );

        if ( middleNode == null )
        {
            middleNode = neo().createNode();
            debug( "\t+Node (middle) " + middleNode );
            Relationship rel = subjectNode.createRelationshipTo( middleNode,
                relationshipType( subjectToMiddle.getRelationshipTypeName() ) );
            debugCreateRelationship( rel );
            applyRepresentation( abstractMiddleNode, middleNode );
            rel = middleNode.createRelationshipTo( objectNode, relationshipType(
                middleToObject.getRelationshipTypeName() ) );
            debugCreateRelationship( rel );
        }
        ensureContextsAreAdded( representation, middleNode, nodeMapping );
    }

    private Node findObjectMiddleNode( Node subjectNode,
        AbstractRelationship subjectToMiddle,
        AbstractRelationship middleToObject, Node objectNode )
    {
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
                if ( anObjectNode.equals( objectNode ) )
                {
                    return aMiddleNode;
                }
            }
        }
        return null;
    }

    private void ensureContextsAreAdded(
        AbstractRepresentation representation, Node middleNode,
        Map<AbstractNode, Node> nodeMapping )
    {
        for ( AbstractRelationship abstractRelationship :
            getContextRelationships( representation, middleNode, nodeMapping ) )
        {
            findContextRelationship( abstractRelationship,
                middleNode, true, nodeMapping );
        }
    }

    private Collection<AbstractRelationship> getContextRelationships(
        AbstractRepresentation representation, Node middleNode,
        Map<AbstractNode, Node> nodeMapping )
    {
        Collection<AbstractRelationship> list =
            new ArrayList<AbstractRelationship>();
        for ( AbstractRelationship abstractRelationship :
            representation.relationships() )
        {
            if ( relationshipIsType( abstractRelationship,
                AlwaysMiddleNodesStrategy.TYPE_MIDDLE,
                AlwaysMiddleNodesStrategy.TYPE_CONTEXT ) )
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
            AlwaysMiddleNodesStrategy.TYPE_LITERAL ) != null;
    }

    private boolean isObjectTypeRepresentation(
        AbstractRepresentation representation )
    {
        return getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_OBJECT ) != null;
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
            AlwaysMiddleNodesStrategy.EXECUTOR_INFO_NODE_TYPE );
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
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        AbstractNode abstractSubjectNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_SUBJECT );
        Node subjectNode = lookupNode( abstractSubjectNode );
        AbstractNode abstractObjectNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_OBJECT );
        Node objectNode = lookupNode( abstractObjectNode );
        if ( subjectNode == null || objectNode == null )
        {
            return;
        }

        AbstractNode abstractMiddleNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_MIDDLE );
        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_SUBJECT,
            AlwaysMiddleNodesStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToObject = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_MIDDLE,
            AlwaysMiddleNodesStrategy.TYPE_OBJECT );

        Node middleNode = findObjectMiddleNode( subjectNode, subjectToMiddle,
            middleToObject, objectNode );
        if ( middleNode == null )
        {
            return;
        }

        Collection<AbstractRelationship> contextRelationships =
            getContextRelationships( representation, middleNode, nodeMapping );
        if ( contextRelationships.isEmpty() )
        {
            // Remove all the contexts (if any) and the literal.
            for ( Relationship relationship : middleNode.getRelationships(
                AlwaysMiddleNodesStrategy.RelTypes.IN_CONTEXT ) )
            {
                relationship.delete();
            }
            disconnectMiddle( middleNode, middleToObject, objectNode,
                subjectNode, subjectToMiddle );
        }
        else
        {
            // Remove the supplied contexts and if there are no more left
            // then remove the literal.
            for ( AbstractRelationship contextRelationship :
                contextRelationships )
            {
                Node contextNode = lookupNode(
                    contextRelationship.getEndNode() );
                if ( contextNode == null )
                {
                    continue;
                }
                Relationship relationship = findDirectRelationship( middleNode,
                    relationshipType(
                    contextRelationship.getRelationshipTypeName() ),
                    contextNode, Direction.OUTGOING );
                if ( relationship != null )
                {
                    debugRemoveRelationship( relationship );
                    relationship.delete();
                }
            }
            if ( !middleNode.hasRelationship(
                AlwaysMiddleNodesStrategy.RelTypes.IN_CONTEXT,
                Direction.OUTGOING ) )
            {
                disconnectMiddle( middleNode, middleToObject, objectNode,
                    subjectNode, subjectToMiddle );
            }
        }

        if ( nodeIsEmpty( abstractSubjectNode, subjectNode, true ) )
        {
            deleteNode( subjectNode, abstractSubjectNode.getUriOrNull() );
        }
        if ( nodeIsEmpty( abstractObjectNode, objectNode, true ) )
        {
            deleteNode( objectNode, abstractObjectNode.getUriOrNull() );
        }
    }

    private void handleRemoveLiteralRepresentation(
        AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        AbstractNode abstractSubjectNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_SUBJECT );
        Node subjectNode = lookupNode( abstractSubjectNode );
        if ( subjectNode == null )
        {
            return;
        }

        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_SUBJECT,
            AlwaysMiddleNodesStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToLiteral = findAbstractRelationship(
            representation, AlwaysMiddleNodesStrategy.TYPE_MIDDLE,
            AlwaysMiddleNodesStrategy.TYPE_LITERAL );
        AbstractNode abstractLiteralNode = getNodeByType( representation,
            AlwaysMiddleNodesStrategy.TYPE_LITERAL );

        Node[] nodes = findLiteralNode( subjectNode, subjectToMiddle,
            middleToLiteral, abstractLiteralNode );
        Node middleNode = nodes[ 0 ];
        Node literalNode = nodes[ 1 ];
        if ( literalNode == null )
        {
            return;
        }

        Collection<AbstractRelationship> contextRelationships =
            getContextRelationships( representation, middleNode, nodeMapping );
        if ( contextRelationships.isEmpty() )
        {
            // Remove all the contexts (if any) and the literal.
            for ( Relationship relationship : middleNode.getRelationships(
                AlwaysMiddleNodesStrategy.RelTypes.IN_CONTEXT ) )
            {
                debugRemoveRelationship( relationship );
                relationship.delete();
            }
            deleteMiddleAndLiteral( middleNode, middleToLiteral,
                literalNode, subjectNode, subjectToMiddle );
        }
        else
        {
            // Remove the supplied contexts and if there are no more left
            // then remove the literal.
            for ( AbstractRelationship contextRelationship :
                contextRelationships )
            {
                Node contextNode = lookupNode(
                    contextRelationship.getEndNode() );
                if ( contextNode == null )
                {
                    continue;
                }
                Relationship relationship = findDirectRelationship( middleNode,
                    relationshipType(
                    contextRelationship.getRelationshipTypeName() ),
                    contextNode, Direction.OUTGOING );
                if ( relationship != null )
                {
                    debugRemoveRelationship( relationship );
                    relationship.delete();
                }
            }
            if ( !middleNode.hasRelationship(
                AlwaysMiddleNodesStrategy.RelTypes.IN_CONTEXT,
                Direction.OUTGOING ) )
            {
                deleteMiddleAndLiteral( middleNode, middleToLiteral,
                    literalNode, subjectNode, subjectToMiddle );
            }
        }

        if ( nodeIsEmpty( abstractSubjectNode, subjectNode, true ) )
        {
            deleteNode( subjectNode, abstractSubjectNode.getUriOrNull() );
        }
    }

    private void deleteMiddleAndLiteral( Node middleNode,
        AbstractRelationship middleToLiteral, Node literalNode,
        Node subjectNode, AbstractRelationship subjectToMiddle )
    {
        disconnectMiddle( middleNode, middleToLiteral, literalNode,
            subjectNode, subjectToMiddle );
        deleteNode( literalNode, null );
    }

    private void disconnectMiddle( Node middleNode,
        AbstractRelationship middleToOther, Node otherNode,
        Node subjectNode, AbstractRelationship subjectToMiddle )
    {
        ensureDirectlyDisconnected( middleNode, relationshipType(
            middleToOther.getRelationshipTypeName() ), otherNode );
        ensureDirectlyDisconnected( subjectNode, relationshipType(
            subjectToMiddle.getRelationshipTypeName() ), middleNode );
        deleteNode( middleNode, null );
    }
}
