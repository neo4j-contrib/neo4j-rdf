package org.neo4j.rdf.store.representation.standard;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.PropertyContainer;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.AbstractElement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;
import org.neo4j.util.NeoPropertyArraySet;
import org.neo4j.util.index.Index;
import org.neo4j.util.index.IndexService;
import org.neo4j.util.matching.PatternMatch;
import org.neo4j.util.matching.PatternMatcher;
import org.neo4j.util.matching.PatternNode;

/**
 * An implementation of {@link RepresentationExecutor} which uses an
 * {@link Index}, where the indexing key is each elements {@link Uri} as a way
 * of looking up the objects.
 * 
 * This is an attempt to implement a generic executor which tries to execute a
 * {@link AbstractRepresentation}s as straight forward and correct as possible.
 */
public class UriBasedExecutor extends AbstractUriBasedExecutor
{
    /**
     * Set on an AbstractNode (with a boolean as value) to let the executor
     * know that the node is a literal node (and should be indexed).
     */
    public static final String EXEC_INFO_IS_LITERAL_NODE = "literal";
    
    /**
     * Set on an AbstractElement (with a Collection<String> as value) containing
     * the keys in that element which should be treated as literals
     * (and hence should be indexed).
     */
    public static final String EXEC_INFO_KEYS_WHICH_ARE_LITERALS =
        "literal_keys";

    /**
     * @param neo the {@link NeoService}.
     * @param index the {@link Index} to use as the lookup for objects.
     */
    public UriBasedExecutor( NeoService neo, IndexService index,
        MetaStructure meta, FulltextIndex fulltextIndex )
    {
        super( neo, index, meta, fulltextIndex );
    }

    public void addToNodeSpace( AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        for ( AbstractNode abstractNode : representation.nodes() )
        {
            if ( abstractNode.getUriOrNull() != null )
            {
                Node node = lookupOrCreateNode(
                	abstractNode, nodeMapping ).getNode();
                applyOnNode( abstractNode, node );
            }
        }

        for ( AbstractRelationship abstractRelationship :
            representation.relationships() )
        {
            Node startNode = nodeMapping.get( abstractRelationship
                .getStartNode() );
            Node endNode = nodeMapping.get( abstractRelationship.getEndNode() );
            RelationshipType relType = new ARelationshipType(
                abstractRelationship.getRelationshipTypeName() );
            Relationship relationship = null;
            if ( startNode != null && endNode != null )
            {
                relationship = ensureConnected( startNode, relType, endNode );
            }
            else if ( startNode == null && endNode == null )
            {
                throw new UnsupportedOperationException(
                    "Both start and end null" );
            }
            else
            {
                NodeAndRelationship result = findOtherNodePresumedBlank(
                    abstractRelationship, representation, nodeMapping, true );
                relationship = result.relationship;
                if ( result.node != null )
                {
                    applyOnNode( result.abstractNode, result.node );
                }
            }

            if ( relationship != null )
            {
                applyOnRelationship( abstractRelationship, relationship );
            }
        }
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
            Node startNode = nodeMapping.get( abstractRelationship
                .getStartNode() );
            Node endNode = nodeMapping.get( abstractRelationship.getEndNode() );
            RelationshipType relType = new ARelationshipType(
                abstractRelationship.getRelationshipTypeName() );
            if ( startNode != null && endNode != null )
            {
                Relationship relationship = findDirectRelationship( startNode,
                    relType, endNode, Direction.OUTGOING );
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
                    abstractRelationship, representation, nodeMapping,
                    false ).node;
                if ( otherNode == null )
                {
                    return;
                }
                Relationship relationship = startNode != null ?
                    findDirectRelationship( startNode, relType, otherNode,
                        Direction.OUTGOING )
                    : findDirectRelationship( otherNode, relType, startNode,
                        Direction.OUTGOING );
                if ( relationship == null )
                {
                    return;
                }
                relationshipMapping.put( abstractRelationship, relationship );
            }
        }
        doActualDeletion( nodeMapping, relationshipMapping );
    }

    protected void doActualDeletion( Map<AbstractNode, Node> nodeMapping,
        Map<AbstractRelationship, Relationship> relationshipMapping )
    {
        // Do the actual deletion
        Set<Node> deletedNodes = new HashSet<Node>();
        for ( Map.Entry<AbstractNode, Node> entry : nodeMapping.entrySet() )
        {
            AbstractNode abstractNode = entry.getKey();
            Node node = entry.getValue();
            removeFromNode( abstractNode, node );
            if ( nodeIsEmpty( abstractNode, node, true ) )
            {
                deleteNode( node, abstractNode.getUriOrNull() );
                deletedNodes.add( node );
            }
        }
        for ( Map.Entry<AbstractRelationship, Relationship> entry :
            relationshipMapping.entrySet() )
        {
            AbstractRelationship abstractRelationship = entry.getKey();
            Relationship relationship = entry.getValue();
            removeFromRelationship( abstractRelationship, relationship );
            if ( relationshipIsEmpty( abstractRelationship, relationship ) )
            {
//                debugDeleteRelationship( relationship );
                relationship.delete();
            }
        }
        for ( Map.Entry<AbstractNode, Node> entry : nodeMapping.entrySet() )
        {
            AbstractNode abstractNode = entry.getKey();
            Node node = entry.getValue();
            if ( !deletedNodes.contains( node ) &&
                nodeIsEmpty( abstractNode, node, true ) )
            {
                deleteNode( node, abstractNode.getUriOrNull() );
            }
        }
    }

    private boolean oneNodeIsBlankAndItsNotEmpty(
        AbstractRelationship abstractRelationship, Relationship relationship )
    {
        if ( abstractRelationship.getStartNode().getUriOrNull() != null &&
            abstractRelationship.getEndNode().getUriOrNull() != null )
        {
            return false;
        }

        boolean blankIsStartNode =
            abstractRelationship.getStartNode().getUriOrNull() == null;
        AbstractNode abstractNode = blankIsStartNode ?
            abstractRelationship.getStartNode() :
            abstractRelationship.getEndNode();
        Node node = blankIsStartNode ?
            relationship.getStartNode() : relationship.getEndNode();
        if ( !nodeIsEmpty( abstractNode, node, false ) )
        {
            return true;
        }
        return false;
    }

    protected boolean relationshipIsEmpty(
        AbstractRelationship abstractRelationship, Relationship relationship )
    {
        if ( oneNodeIsBlankAndItsNotEmpty(
            abstractRelationship, relationship ) )
        {
            return false;
        }

        AbstractNode abstractStartNode = abstractRelationship.getStartNode();
        if ( abstractStartNode.getUriOrNull() == null && !nodeIsEmpty(
            abstractStartNode, relationship.getStartNode(), false ) )
        {
            return false;
        }
        boolean hasProperties =
            relationship.getPropertyKeys().iterator().hasNext();
        return !hasProperties;
    }

    private Relationship ensureConnected( Node startNode,
        RelationshipType relType, Node endNode )
    {
        Relationship relationship =
            findDirectRelationship( startNode, relType, endNode, null );
        if ( relationship == null )
        {
            relationship = startNode.createRelationshipTo( endNode, relType );
//            debug( "\t+Relationship " + startNode + " ---["
//                + relType.name() + "]--> " + endNode );
        }
        return relationship;
    }

    private void applyOnRelationship( AbstractRelationship abstractRelationship,
        Relationship relationship )
    {
        applyOnContainer( abstractRelationship, relationship );
    }

    protected void applyOnNode( AbstractNode abstractNode, Node node )
    {
        applyOnContainer( abstractNode, node );
    }

    private void applyOnContainer( AbstractElement abstractElement,
        PropertyContainer container )
    {
        Collection<?> keysWhichAreLiterals = ( Collection<?> )
            abstractElement.getExecutorInfo(
                EXEC_INFO_KEYS_WHICH_ARE_LITERALS );
        
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractElement.properties().entrySet() )
        {
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo(), container, entry.getKey() );
            for ( Object value : entry.getValue() )
            {
                boolean added = neoValues.add( value );
                if ( added )
                {
//                  debug( "\t+Property" + " (" + container + ") "
//                  + entry.getKey() + " " + "[" + value + "]" );
                    if ( keysWhichAreLiterals != null &&
                        keysWhichAreLiterals.contains( entry.getKey() ) )
                    {
                        indexLiteral( ( Node ) container,
                            new Uri( entry.getKey() ), value );
                    }
                }
            }
        }
    }

    protected void removeFromRelationship(
        AbstractRelationship abstractRelationship, Relationship relationship )
    {
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractRelationship.properties().entrySet() )
        {
            String key = entry.getKey();
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo(), relationship, key );
            removeAll( relationship, key, neoValues,
                entry.getValue(), "Property" );
        }
    }

    protected boolean removeAll( PropertyContainer container, String key,
        Collection<Object> neoValues, Collection<?> valuesToRemove,
        String debugText )
    {
        boolean someRemoved = false;
        for ( Object value : valuesToRemove )
        {
            boolean removed = neoValues.remove( value );
            if ( removed )
            {
                someRemoved = true;
//                debug( "\t-" + debugText + " (" + container + ") "
//                    + key + " " + "[" + value + "]" );
            }
        }
        return someRemoved;
    }

    protected void removeFromNode( AbstractNode abstractNode, Node node )
    {
        Collection<?> keysWhichAreLiterals = ( Collection<?> )
            abstractNode.getExecutorInfo( EXEC_INFO_KEYS_WHICH_ARE_LITERALS );
        
        // Do the real keys
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractNode.properties().entrySet() )
        {
            String key = entry.getKey();
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo(), node, key );
            for ( Object value : entry.getValue() )
            {
                boolean removed = neoValues.remove( value );
                if ( removed )
                {
                    if ( keysWhichAreLiterals != null &&
                        keysWhichAreLiterals.contains( entry.getKey() ) )
                    {
                        removeLiteralIndex( node, new Uri( key ), value );
                    }
//                    debug( "\t-Property"  + " (" + node + ") "
//                        + key + " " + "[" + value + "]" );
                }
            }
        }
    }

    private NodeAndRelationship findOtherNodePresumedBlank(
        AbstractRelationship abstractRelationship,
        AbstractRepresentation representation,
        Map<AbstractNode, Node> nodeMapping, boolean createIfItDoesntExist )
    {
        Map<AbstractNode, PatternNode> patternNodes =
            representationToPattern( representation );
        AbstractNode startingAbstractNode = abstractRelationship.getStartNode()
            .getUriOrNull() == null ? abstractRelationship.getEndNode()
            : abstractRelationship.getStartNode();
        AbstractNode endingAbstractNode =
            abstractRelationship.getOtherNode( startingAbstractNode );
        Node startingNode = nodeMapping.get( startingAbstractNode );
        PatternNode startingPatternNode = patternNodes
            .get( startingAbstractNode );
        Iterator<PatternMatch> matches = PatternMatcher.getMatcher().match(
            startingPatternNode, startingNode, null ).iterator();
        Node node = null;
        Relationship relationship = null;
        if ( matches.hasNext() )
        {
            PatternMatch match = matches.next();
            node = match.getNodeFor(
                patternNodes.get( endingAbstractNode ) );
            Node otherNode =
                match.getNodeFor( patternNodes.get( startingAbstractNode ) );
            relationship = findDirectRelationship( node, relationshipType(
                abstractRelationship.getRelationshipTypeName() ), otherNode,
                null );
        }
        else if ( createIfItDoesntExist )
        {
            NodeAndRelationship createdNodeAndRelationship =
                createBlankNodeIfDoesntExist( startingAbstractNode,
                    endingAbstractNode, abstractRelationship, nodeMapping );
            node = createdNodeAndRelationship.node;
            relationship = createdNodeAndRelationship.relationship;
        }
        nodeMapping.put( endingAbstractNode, node );
        return new NodeAndRelationship( endingAbstractNode,
            node, relationship );
    }

    protected NodeAndRelationship createBlankNodeIfDoesntExist(
        AbstractNode startNode, AbstractNode endNode,
        AbstractRelationship abstractRelationship,
        Map<AbstractNode, Node> nodeMapping )
    {
        Node node = neo().createNode();
        Relationship relationship = null;
//        debug( "\tNo match, creating node (" + node.getId()
//            + ") and connecting" );
        RelationshipType relationshipType = new ARelationshipType(
            abstractRelationship.getRelationshipTypeName() );
        if ( abstractRelationship.getStartNode().getUriOrNull() == null )
        {
            relationship = node.createRelationshipTo(
                nodeMapping.get( abstractRelationship.getEndNode() ),
                    relationshipType );
//            debug( "\t+Relationship " + node + " ---["
//                + relationshipType.name() + "]--> "
//                + nodeMapping.get( abstractRelationship.getEndNode() ) );
        }
        else
        {
            relationship = nodeMapping.get(
                abstractRelationship.getStartNode() ).createRelationshipTo(
                    node, relationshipType );
//            debug( "\t+Relationship "
//                + nodeMapping.get( abstractRelationship.getStartNode() )
//                + " ---[" + relationshipType.name() + "]--> " + node );
        }
        return new NodeAndRelationship( null, node, relationship );
    }

    private Map<AbstractNode, PatternNode> representationToPattern(
        AbstractRepresentation representation )
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

    private PatternNode abstractNodeToPatternNode( AbstractNode node )
    {
        PatternNode patternNode = new PatternNode();
        if ( node.getUriOrNull() != null )
        {
            patternNode.addPropertyEqualConstraint(
                getNodeUriPropertyKey( node ),
                node.getUriOrNull().getUriAsString() );
        }

        if ( isLiteralNode( node ) )
        {
            for ( Map.Entry<String, Collection<Object>> entry :
                node.properties().entrySet() )
            {
                patternNode.addPropertyEqualConstraint( entry.getKey(),
                    entry.getValue().toArray() );
            }
        }
        return patternNode;
    }

    protected boolean isLiteralNode( AbstractNode node )
    {
        Boolean isLiteralNode = ( Boolean )
        node.getExecutorInfo( EXEC_INFO_IS_LITERAL_NODE );
        return isLiteralNode != null && isLiteralNode;
    }

    public static class ARelationshipType implements RelationshipType
    {
        private String name;

        public ARelationshipType( String name )
        {
            this.name = name;
        }

        public String name()
        {
            return this.name;
        }
    }

    protected static class NodeAndRelationship
    {
        private AbstractNode abstractNode;
        private Node node;
        private Relationship relationship;

        NodeAndRelationship( AbstractNode abstractNode,
            Node node, Relationship relationship )
        {
            this.abstractNode = abstractNode;
            this.node = node;
            this.relationship = relationship;
        }
    }
}
