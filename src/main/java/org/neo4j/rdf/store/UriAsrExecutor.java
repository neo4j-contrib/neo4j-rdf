package org.neo4j.rdf.store;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;
import org.neo4j.rdf.store.representation.AsrExecutor;
import org.neo4j.util.NeoPropertyArraySet;
import org.neo4j.util.PureNodeRelationshipSet;
import org.neo4j.util.index.Index;
import org.neo4j.util.matching.PatternMatch;
import org.neo4j.util.matching.PatternMatcher;
import org.neo4j.util.matching.PatternNode;

/**
 * An implementation of {@link AsrExecutor} which uses an {@link Index},
 * where the indexing key is each elements {@link Uri} as a way of looking up
 * the objects.
 */
public class UriAsrExecutor implements AsrExecutor
{
    static final String URI_PROPERTY_KEY = "uri";

    private NeoService neo;
    private Index index;

    /**
     * @param neo the {@link NeoService}.
     * @param index the {@link Index} to use as the lookup for objects.
     */
    public UriAsrExecutor( NeoService neo, Index index )
    {
        this.neo = neo;
        this.index = index;
    }

    protected void debug( String message )
    {
//        System.out.println( message );
    }

    protected Node lookupNode( AbstractNode node,
        boolean createIfItDoesntExist )
    {
        String uri = node.getUriOrNull().uriAsString();
        Node result = index.getSingleNodeFor( uri );
        if ( result == null && createIfItDoesntExist )
        {
            result = neo.createNode();
            result.setProperty( URI_PROPERTY_KEY, uri );
            index.index( result, uri );
            debug( "\t+Node (" + result.getId() + ") '" + uri + "'" );
        }
//        else if ( result != null )
//        {
//            debug( "\tIndex has node (" + result.getId() + ")" );
//        }
        return result;
    }
    
    private void removeNode( Node node, Uri uri )
    {
        node.delete();
        if ( uri != null )
        {
            this.index.remove( node, uri.uriAsString() );
        }
    }

    public void addToNodeSpace( AbstractStatementRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        for ( AbstractNode abstractNode : representation.nodes() )
        {
            if ( abstractNode.getUriOrNull() != null )
            {
                Node node = lookupNode( abstractNode, true );
                applyOnNode( abstractNode, node );
                nodeMapping.put( abstractNode, node );
            }
        }

        for ( AbstractRelationship abstractRelationship : representation
            .relationships() )
        {
            Node startNode = nodeMapping.get( abstractRelationship
                .getStartNode() );
            Node endNode = nodeMapping.get( abstractRelationship.getEndNode() );
            RelationshipType relType = new ARelationshipType(
                abstractRelationship.getRelationshipTypeName() );
            if ( startNode != null && endNode != null )
            {
                ensureConnected( startNode, relType, endNode );
            }
            else if ( startNode == null && endNode == null )
            {
                throw new UnsupportedOperationException(
                    "Both start and end null" );
            }
            else
            {
                findOtherNodePresumedBlank( abstractRelationship,
                    representation, nodeMapping, true );
            }
        }
    }

    public void removeFromNodeSpace(
        AbstractStatementRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        for ( AbstractNode abstractNode : representation.nodes() )
        {
            if ( abstractNode.getUriOrNull() != null )
            {
                Node node = lookupNode( abstractNode, false );
                if ( node == null )
                {
                    return;
                }
                nodeMapping.put( abstractNode, node );
            }
        }

        Map<AbstractRelationship, Relationship> relationshipMapping =
            new HashMap<AbstractRelationship, Relationship>();
        for ( AbstractRelationship abstractRelationship : representation
            .relationships() )
        {
            Node startNode = nodeMapping.get( abstractRelationship
                .getStartNode() );
            Node endNode = nodeMapping.get( abstractRelationship.getEndNode() );
            RelationshipType relType = new ARelationshipType(
                abstractRelationship.getRelationshipTypeName() );
            if ( startNode != null && endNode != null )
            {
                Relationship relationship = lookupRelationship( startNode,
                    relType, endNode );
                if ( relationship == null )
                {
                    return;
                }
                relationshipMapping.put( abstractRelationship, relationship );
            }
            else if ( startNode == null && endNode == null )
            {
                throw new UnsupportedOperationException(
                    "Both start and end null" );
            }
            else
            {
                Node otherNode = findOtherNodePresumedBlank(
                    abstractRelationship, representation, nodeMapping, false );
                if ( otherNode == null )
                {
                    return;
                }
                Relationship relationship = startNode != null ?
                    lookupRelationship( startNode, relType, otherNode )
                    : lookupRelationship( otherNode, relType, startNode );
                if ( relationship == null )
                {
                    return;
                }
                relationshipMapping.put( abstractRelationship, relationship );
            }
        }

        for ( Relationship relationship : relationshipMapping.values() )
        {
            debug( "\t-Relationship "
                + relationship.getStartNode() + " ---["
                + relationship.getType().name() + "]--> "
                + relationship.getEndNode() );
            relationship.delete();
        }
        for ( Map.Entry<AbstractNode, Node> entry : nodeMapping.entrySet() )
        {
            AbstractNode abstractNode = entry.getKey();
            Node node = entry.getValue();
            removeFromNode( abstractNode, node );
            if ( nodeIsEmpty( abstractNode, node ) )
            {
                debug( "\t-Node " + node );
                removeNode( node, abstractNode.getUriOrNull() );
            }
        }
    }

    private boolean nodeIsEmpty( AbstractNode abstractNode, Node node )
    {
        if ( node.hasRelationship() )
        {
            return false;
        }
        for ( String key : node.getPropertyKeys() )
        {
            if ( !key.equals( getNodeUriProperty( abstractNode ) ) )
            {
                return false;
            }
        }
        return true;
    }

    private Relationship lookupRelationship( Node startNode,
        RelationshipType relType, Node endNode )
    {
        for ( Relationship rel : startNode.getRelationships( relType,
            Direction.OUTGOING ) )
        {
            if ( rel.getOtherNode( startNode ).equals( endNode ) )
            {
                return rel;
            }
        }
        return null;
    }

    private void ensureConnected( Node startNode, RelationshipType relType,
        Node endNode )
    {
        boolean added = new PureNodeRelationshipSet( startNode, relType )
            .add( endNode );
        if ( added )
        {
            debug( "\t+Relationship " + startNode + " ---["
                + relType.name() + "]--> " + endNode );
        }
    }

    private void applyOnNode( AbstractNode abstractNode, Node node )
    {
        for ( Map.Entry<String, Object> entry : abstractNode.properties()
            .entrySet() )
        {
            boolean added = new NeoPropertyArraySet<Object>( neo, node, entry
                .getKey() ).add( entry.getValue() );
            if ( added )
            {
                debug( "\t+Property (" + node + ") "
                    + entry.getKey() + " " + "[" + entry.getValue() + "]" );
            }
        }
    }

    private void removeFromNode( AbstractNode abstractNode, Node node )
    {
        for ( Map.Entry<String, Object> entry : abstractNode.properties()
            .entrySet() )
        {
            boolean removed = new NeoPropertyArraySet<Object>( neo, node, entry
                .getKey() ).remove( entry.getValue() );
            if ( removed )
            {
                debug( "\t-Property (" + node + ") "
                    + entry.getKey() + " " + "[" + entry.getValue() + "]" );
            }
        }
    }

    private Node findOtherNodePresumedBlank(
        AbstractRelationship abstractRelationship,
        AbstractStatementRepresentation representation,
        Map<AbstractNode, Node> nodeMapping, boolean createIfItDoesntExist )
    {
        Map<AbstractNode, PatternNode> patternNodes =
            representationToPattern( representation );
        AbstractNode startingAbstractNode = abstractRelationship.getStartNode()
            .getUriOrNull() == null ? abstractRelationship.getEndNode()
            : abstractRelationship.getStartNode();
        Node startingNode = nodeMapping.get( startingAbstractNode );
        PatternNode startingPatternNode = patternNodes
            .get( startingAbstractNode );
        Iterator<PatternMatch> matches = PatternMatcher.getMatcher().match(
            startingPatternNode, startingNode ).iterator();
        Node node = null;
        if ( matches.hasNext() )
        {
            node = matches.next().getNodeFor(
                patternNodes.get( abstractRelationship
                    .getOtherNode( startingAbstractNode ) ) );
        }
        else if ( createIfItDoesntExist )
        {
            node = neo.createNode();
            debug( "\tNo match, creating node (" + node.getId()
                + ") and connecting" );
            RelationshipType relationshipType = new ARelationshipType(
                abstractRelationship.getRelationshipTypeName() );
            if ( abstractRelationship.getStartNode().getUriOrNull() == null )
            {
                node
                    .createRelationshipTo( nodeMapping
                        .get( abstractRelationship.getEndNode() ),
                        relationshipType );
                debug( "\t+Relationship " + node + " ---["
                    + relationshipType.name() + "]--> "
                    + nodeMapping.get( abstractRelationship.getEndNode() ) );
            }
            else
            {
                nodeMapping.get( abstractRelationship.getStartNode() )
                    .createRelationshipTo( node, relationshipType );
                debug( "\t+Relationship "
                    + nodeMapping.get( abstractRelationship.getStartNode() )
                    + " ---[" + relationshipType.name() + "]--> " + node );
            }
        }
        nodeMapping.put( abstractRelationship
            .getOtherNode( startingAbstractNode ), node );
        return node;
    }

    private Map<AbstractNode, PatternNode> representationToPattern(
        AbstractStatementRepresentation representation )
    {
        Map<AbstractNode, PatternNode> patternNodes =
            new HashMap<AbstractNode, PatternNode>();
        for ( AbstractNode node : representation.nodes() )
        {
            PatternNode patternNode = abstractNodeToPatternNode( node );
            patternNodes.put( node, patternNode );
        }

        for ( AbstractRelationship relationship : representation
            .relationships() )
        {
            PatternNode startNode = patternNodes.get( relationship
                .getStartNode() );
            PatternNode endNode = patternNodes.get( relationship.getEndNode() );
            startNode.createRelationshipTo( endNode, new ARelationshipType(
                relationship.getRelationshipTypeName() ) );
        }
        return patternNodes;
    }

    protected String getNodeUriProperty( AbstractNode node )
    {
        return URI_PROPERTY_KEY;
    }

    private PatternNode abstractNodeToPatternNode( AbstractNode node )
    {
        PatternNode patternNode = new PatternNode();
        if ( node.getUriOrNull() != null )
        {
            patternNode.addPropertyEqualConstraint( getNodeUriProperty( node ),
                node.getUriOrNull().uriAsString() );
        }
        for ( Map.Entry<String, Object> entry : node.properties().entrySet() )
        {
            patternNode.addPropertyEqualConstraint( entry.getKey(), entry
                .getValue() );
        }
        return patternNode;
    }

    private static class ARelationshipType implements RelationshipType
    {
        private String name;

        ARelationshipType( String name )
        {
            this.name = name;
        }

        public String name()
        {
            return this.name;
        }
    }
}
