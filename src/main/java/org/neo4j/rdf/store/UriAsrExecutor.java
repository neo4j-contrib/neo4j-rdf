package org.neo4j.rdf.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.PropertyContainer;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.AbstractElement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;
import org.neo4j.rdf.store.representation.AsrExecutor;
import org.neo4j.util.NeoPropertyArraySet;
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
    public static final String CONTEXT_DELIMITER = "š";
    public static final String LOOKUP_CONTEXT_KEYS = "contextKeys";
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
        String uri = node.getUriOrNull().getUriAsString();
        Node result = index.getSingleNodeFor( uri );
        if ( result == null && createIfItDoesntExist )
        {
            result = neo.createNode();
            result.setProperty( URI_PROPERTY_KEY, uri );
            index.index( result, uri );
            debug( "\t+Node (" + result.getId() + ") '" + uri + "'" );
        }
        return result;
    }
    
    private void removeNode( Node node, Uri uri )
    {
        node.delete();
        if ( uri != null )
        {
            this.index.remove( node, uri.getUriAsString() );
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
                relationship = findOtherNodePresumedBlank( abstractRelationship,
                    representation, nodeMapping, true ).relationship;
            }
            applyOnRelationship( abstractRelationship, relationship );
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
                    abstractRelationship, representation, nodeMapping,
                    false ).node;
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

        // Do the actual deletion
        for ( Map.Entry<AbstractRelationship, Relationship> entry :
            relationshipMapping.entrySet() )
        {
            AbstractRelationship abstractRelationship = entry.getKey();
            Relationship relationship = entry.getValue();
            removeFromRelationship( abstractRelationship, relationship );
            if ( relationshipIsEmpty( abstractRelationship, relationship ) )
            {
                relationship.delete();
                debug( "\t-Relationship "
                    + relationship.getStartNode() + " ---["
                    + relationship.getType().name() + "]--> "
                    + relationship.getEndNode() );
            }
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
    
    protected String getPropertyKeyForContextKey( AbstractElement element,
        String contextKey )
    {
        Map<?, ?> contextToPropertyKeys = ( Map<?, ?> ) element.lookupInfo(
            UriAsrExecutor.LOOKUP_CONTEXT_KEYS );
        return contextToPropertyKeys == null ? null :
            ( String ) contextToPropertyKeys.get( contextKey );
    }
    
    protected boolean isContextKey( AbstractElement element,
        String key )
    {
        Map<?, ?> contextToPropertyKeys = ( Map<?, ?> ) element.lookupInfo(
            UriAsrExecutor.LOOKUP_CONTEXT_KEYS );
        return contextToPropertyKeys != null &&
            contextToPropertyKeys.containsKey( key );
    }
    
    private boolean relationshipIsEmpty(
        AbstractRelationship abstractRelationship, Relationship relationship )
    {
        return !relationship.getPropertyKeys().iterator().hasNext();
    }

    private boolean nodeIsEmpty( AbstractNode abstractNode, Node node )
    {
        if ( node.hasRelationship() )
        {
            return false;
        }
        for ( String key : node.getPropertyKeys() )
        {
            if ( !key.equals( getNodeUriPropertyKey( abstractNode ) ) )
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

    private Relationship ensureConnected( Node startNode,
        RelationshipType relType, Node endNode )
    {
        Relationship relationship = null;
        for ( Relationship rel :
            startNode.getRelationships( relType, Direction.OUTGOING ) )
        {
            if ( rel.getEndNode().equals( endNode ) )
            {
                relationship = rel;
                break;
            }
        }
        
        if ( relationship == null )
        {
            relationship = startNode.createRelationshipTo( endNode, relType );
            debug( "\t+Relationship " + startNode + " ---["
                + relType.name() + "]--> " + endNode );
        }
        return relationship;
    }
    
    private void applyOnRelationship( AbstractRelationship abstractRelationship,
        Relationship relationship )
    {
        applyOnContainer( abstractRelationship, relationship );
    }

    private void applyOnNode( AbstractNode abstractNode, Node node )
    {
        applyOnContainer( abstractNode, node );
    }
    
    private void applyOnContainer( AbstractElement abstractElement,
        PropertyContainer container )
    {
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractElement.properties().entrySet() )
        {
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo, container, entry.getKey() );
            for ( Object value : entry.getValue() )
            {
                boolean added = neoValues.add( value );
                if ( added )
                {
                    debug( "\t+Property" + " (" + container + ") "
                        + entry.getKey() + " " + "[" + value + "]" );
                }
            }
        }
    }
    
    private void removeFromRelationship(
        AbstractRelationship abstractRelationship, Relationship relationship )
    {
        // We only support context properties on relationship for now.
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractRelationship.properties().entrySet() )
        {
            String key = entry.getKey();
            if ( !isContextKey( abstractRelationship, key ) )
            {
                throw new UnsupportedOperationException( key );
            }
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo, relationship, key );
            removeAll( relationship, key, neoValues,
                entry.getValue(), "Context" );
        }
    }
    
    private void removeAll( PropertyContainer container, String key,
        Collection<Object> neoValues, Collection<?> valuesToRemove,
        String debugText )
    {
        for ( Object value : valuesToRemove )
        {
            boolean removed = neoValues.remove( value );
            if ( removed )
            {
                debug( "\t-" + debugText + " (" + container + ") "
                    + key + " " + "[" + value + "]" );
            }
        }
    }

    private void removeFromNode( AbstractNode abstractNode, Node node )
    {
        // Take the context properties first.
        Map<String, String> contextToReal = new HashMap<String, String>();
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractNode.properties().entrySet() )
        {
            String key = entry.getKey();
            if ( !isContextKey( abstractNode, key ) )
            {
                continue;
            }
            String realKey =
                getPropertyKeyForContextKey( abstractNode, key );
            if ( realKey != null )
            {
                contextToReal.put( key, realKey );
            }
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo, node, key );
            removeAll( node, key, neoValues, entry.getValue(), "Context" );
        }
        
        // Do the real keys
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractNode.properties().entrySet() )
        {
            String key = entry.getKey();
            if ( isContextKey( abstractNode, key ) )
            {
                continue;
            }
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo, node, key );
            String contextKey = contextToReal.get( key );
            if ( contextKey != null )
            {
                // This means that we're trying to remove contexts, not the
                // actual property, unless all the contexts has been removed.
                for ( Object value : entry.getValue() )
                {
                    if ( !thereAreMoreContexts( node, key, neoValues ) )
                    {
                        boolean removed = neoValues.remove( value );
                        if ( removed )
                        {
                            debug( "\t-Property"  + " (" + node + ") "
                                + key + " " + "[" + value + "]" );
                        }
                    }
                }
            }
            else
            {
                // This means that we're removing the property and all its
                // contexts as well.
                for ( Object value : entry.getValue() )
                {
                    if ( node.removeProperty( formContextPropertyKey(
                        key, value ) ) != null )
                    {
                        debug( "\t-" + "Contexts" + " (" + node + ") "
                            + key + " " + "[" + value + "]" );
                    }
                    boolean removed = neoValues.remove( value );
                    if ( removed )
                    {
                        debug( "\t-Property"  + " (" + node + ") "
                            + key + " " + "[" + value + "]" );
                    }
                }
            }
        }
    }
    
    private boolean thereAreMoreContexts( Node node, String key,
        Collection<Object> neoValues )
    {
        for ( Object value : neoValues )
        {
            String contextKey = formContextPropertyKey( key, value );
            if ( node.hasProperty( contextKey ) )
            {
                return true;
            }
        }
        return false;
    }

    private NodeAndRelationship findOtherNodePresumedBlank(
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
        Relationship relationship = null;
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
                relationship = node.createRelationshipTo(
                    nodeMapping.get( abstractRelationship.getEndNode() ),
                        relationshipType );
                debug( "\t+Relationship " + node + " ---["
                    + relationshipType.name() + "]--> "
                    + nodeMapping.get( abstractRelationship.getEndNode() ) );
            }
            else
            {
                relationship = nodeMapping.get(
                    abstractRelationship.getStartNode() ).createRelationshipTo(
                        node, relationshipType );
                debug( "\t+Relationship "
                    + nodeMapping.get( abstractRelationship.getStartNode() )
                    + " ---[" + relationshipType.name() + "]--> " + node );
            }
            applyOnRelationship( abstractRelationship, relationship );
        }
        nodeMapping.put( abstractRelationship
            .getOtherNode( startingAbstractNode ), node );
        return new NodeAndRelationship( node, relationship );
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

    private PatternNode abstractNodeToPatternNode( AbstractNode node )
    {
        PatternNode patternNode = new PatternNode();
        if ( node.getUriOrNull() != null )
        {
            patternNode.addPropertyEqualConstraint(
                getNodeUriPropertyKey( node ),
                node.getUriOrNull().getUriAsString() );
        }
//        for ( Map.Entry<String, Collection<Object>> entry :
//            node.properties().entrySet() )
//        {
//            patternNode.addPropertyEqualConstraint( entry.getKey(),
//                entry.getValue().toArray() );
//        }
        return patternNode;
    }
    
    public Node lookupNode( AbstractNode abstractNode )
    {
        if ( abstractNode.getUriOrNull() == null )
        {
            return null;
        }
        return this.index.getSingleNodeFor(
            abstractNode.getUriOrNull().getUriAsString() );
    }
    
    public String getNodeUriPropertyKey( AbstractNode abstractNode )
    {
        return URI_PROPERTY_KEY;
    }

    public static String formContextPropertyKey( String predicate,
        Object value )
    {
        return predicate + CONTEXT_DELIMITER + value.toString();
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
    
    private static class NodeAndRelationship
    {
        private Node node;
        private Relationship relationship;
        
        NodeAndRelationship( Node node, Relationship relationship )
        {
            this.node = node;
            this.relationship = relationship;
        }
    }
}
