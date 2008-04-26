package org.neo4j.rdf.store.representation.standard;

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
import org.neo4j.util.index.Index;

public class PureQuadRepresentationExecutor extends AbstractUriBasedExecutor
{
    public static final String RDF_TYPE_URI =
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String LITERAL_VALUE_KEY = "value";

    public PureQuadRepresentationExecutor( NeoService neo, Index index, MetaStructure meta )
    {
        super( neo, index, meta );
    }

    public void addToNodeSpace( AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> abstractNodeToNodeMap =
            new HashMap<AbstractNode, Node>();
        for ( AbstractRelationship abstractRelationship :
            representation.relationships() )
        {
            AbstractNode abstractStartNode =
                abstractRelationship.getStartNode();
            AbstractNode abstractEndNode = abstractRelationship.getEndNode();
            Relationship relationship = null;
            if ( getNodeUri( abstractStartNode ) != null &&
                getNodeUri( abstractEndNode ) != null )
            {
                // Its a connection to another resource.
                Node startNode = lookupOrCreateNode( abstractRelationship.
                    getStartNode(), abstractNodeToNodeMap );
                Node endNode = lookupOrCreateNode(
                    abstractRelationship.getEndNode(), abstractNodeToNodeMap );
                RelationshipType relationshipType = relationshipType(
                    abstractRelationship.getRelationshipTypeName() );
                relationship = ensureDirectlyConnected( startNode,
                    relationshipType, endNode );
            }
            else if ( getNodeUri( abstractEndNode ) == null )
            {
                // It's a literal.
                relationship = ensureLiteralRelationshipExists(
                    abstractRelationship, abstractNodeToNodeMap );
            }
            else
            {
                throw new RuntimeException( "This executor doesn't support " +
                    "the given representation" );
            }
            applyRepresentation( abstractRelationship, relationship );
        }

        for ( AbstractNode abstractNode : representation.nodes() )
        {
            Node node = abstractNodeToNodeMap.get( abstractNode );
            if ( node == null )
            {
                throw new RuntimeException( "Hmm, a bug?" );
            }
            applyRepresentation( abstractNode, node );
        }
    }

    protected Relationship ensureLiteralRelationshipExists(
        AbstractRelationship abstractRelationship,
        Map<AbstractNode, Node> abstractNodeToNodeMap )
    {
        Node startNode = lookupOrCreateNode(
            abstractRelationship.getStartNode(), abstractNodeToNodeMap );
        RelationshipType relationshipType = relationshipType(
            abstractRelationship.getRelationshipTypeName() );
        Relationship relationship = findLiteralRelationship( startNode,
            relationshipType, abstractRelationship.getEndNode().properties() );
        Node literalNode = null;
        if ( relationship == null )
        {
            literalNode = neo().createNode();
            debug( "\t+Node (literal) " + literalNode + " " +
                relationshipType.name() );
            relationship = startNode.createRelationshipTo( literalNode,
                relationshipType );
            debugCreateRelationship( relationship );
        }
        else
        {
            literalNode = relationship.getEndNode();
        }
        abstractNodeToNodeMap.put( abstractRelationship.getEndNode(),
            literalNode );
        return relationship;
    }

    protected Relationship findLiteralRelationship( Node node,
        RelationshipType relationshipType,
        Map<String, Collection<Object>> containingProperties )
    {
        for ( Relationship relationship :
            node.getRelationships( relationshipType, Direction.OUTGOING ) )
        {
            Node literalNode = relationship.getOtherNode( node );
            if ( containsProperties( literalNode, containingProperties ) )
            {
                return relationship;
            }
        }
        return null;
    }

    public void removeFromNodeSpace( AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping =
            getWellKnownNodeMappings( representation );
        if ( nodeMapping == null )
        {
            return;
        }

        Map<AbstractRelationship, Relationship> relationshipMapping =
            new HashMap<AbstractRelationship, Relationship>();
        for ( AbstractRelationship abstractRelationship :
            representation.relationships() )
        {
            AbstractNode abstractStartNode =
                abstractRelationship.getStartNode();
            AbstractNode abstractEndNode = abstractRelationship.getEndNode();
            Relationship relationship = null;
            if ( getNodeUri( abstractStartNode ) != null &&
                getNodeUri( abstractEndNode ) != null )
            {
                // Its a connection to another resource.
                Node startNode = nodeMapping.get( abstractStartNode );
                Node endNode = nodeMapping.get( abstractEndNode );
                RelationshipType relationshipType = relationshipType(
                    abstractRelationship.getRelationshipTypeName() );
                relationship = findDirectRelationship( startNode,
                    relationshipType, endNode, Direction.OUTGOING );
            }
            else if ( getNodeUri( abstractEndNode ) == null )
            {
                // It's a literal.
                Node startNode = nodeMapping.get( abstractStartNode );
                RelationshipType relationshipType = relationshipType(
                    abstractRelationship.getRelationshipTypeName() );
                relationship = findLiteralRelationship( startNode,
                    relationshipType, abstractEndNode.properties() );
                if ( relationship != null )
                {
                    nodeMapping.put( abstractEndNode,
                        relationship.getEndNode() );
                }
            }
            else
            {
                throw new RuntimeException( "This executor doesn't support " +
                    "the given representation" );
            }

            if ( relationship == null )
            {
                return;
            }
            relationshipMapping.put( abstractRelationship, relationship );
        }

        for ( Map.Entry<AbstractRelationship, Relationship> entry :
            relationshipMapping.entrySet() )
        {
            AbstractRelationship abstractRelationship = entry.getKey();
            Relationship relationship = entry.getValue();
            removeFromRelationship( abstractRelationship, relationship,
                nodeMapping );
        }

        for ( Map.Entry<AbstractNode, Node> entry :
            nodeMapping.entrySet() )
        {
            if ( entry.getKey().getUriOrNull() != null )
            {
                removeRepresentation( entry.getKey(), entry.getValue() );
                if ( nodeIsEmpty( entry.getKey(), entry.getValue(), true ) )
                {
                    deleteNode( entry.getValue(),
                        entry.getKey().getUriOrNull() );
                }
            }
        }
    }

    private void removeFromRelationship(
        AbstractRelationship abstractRelationship, Relationship relationship,
        Map<AbstractNode, Node> nodeMapping )
    {
        boolean triedToRemoveSomeContext = false;
        boolean removedSomeContext = false;
        for ( String key : abstractRelationship.properties().keySet() )
        {
            triedToRemoveSomeContext = true;
            if ( removeRepresentation( abstractRelationship,
                relationship, key ) )
            {
                removedSomeContext = true;
            }
        }

        if ( triedToRemoveSomeContext && !removedSomeContext )
        {
            return;
        }

        AbstractNode endNode = abstractRelationship.getEndNode();
        if ( !triedToRemoveSomeContext || contextRelationshipIsEmpty(
            abstractRelationship, relationship ) )
        {
            debugRemoveRelationship( relationship );
            relationship.delete();
            if ( endNode.getUriOrNull() == null )
            {
                deleteNode( nodeMapping.get( endNode ), null );
            }
        }
    }
}
