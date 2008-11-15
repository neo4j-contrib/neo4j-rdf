package org.neo4j.rdf.store.representation.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.fulltext.FulltextIndex;
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
    public static final String STATEMENT_COUNT = "context_statement_count";
//    public static final String SUBJECT_ENERGY = "subject_energy";
//    public static final String OBJECT_ENERGY = "object_energy";
    public static final String IS_CONTEXT_KEY = "is_context";
    
    private static final Collection<String> EXCLUDED_LITERAL_KEYS =
    	new HashSet<String>();
    static
    {
    	EXCLUDED_LITERAL_KEYS.add( LITERAL_DATATYPE_KEY );
    	EXCLUDED_LITERAL_KEYS.add( LITERAL_LANGUAGE_KEY );
    }
    
    public static enum RelTypes implements RelationshipType
    {
    	REF_CONTEXTS,
    	IS_A_CONTEXT,
    }
    
    private Node contextRefNodeCache;

    public VerboseQuadExecutor( NeoService neo, IndexService index,
        MetaStructure meta, FulltextIndex fulltextIndex )
    {
        super( neo, index, meta, fulltextIndex );
    }
    
    public Node getContextsReferenceNode()
    {
        if ( this.contextRefNodeCache == null )
        {
            this.contextRefNodeCache = this.neoUtil().
                getOrCreateSubReferenceNode( RelTypes.REF_CONTEXTS );
        }
        return this.contextRefNodeCache;
    }

    @Override
    public void addToNodeSpace( AbstractRepresentation representation )
    {
        Map<String, AbstractNode> typeToNode =
            getTypeToNodeMap( representation );
        if ( isLiteralRepresentation( typeToNode ) )
        {
            handleAddLiteralRepresentation( representation, typeToNode );
        }
        else if ( isObjectTypeRepresentation( typeToNode ) )
        {
            handleAddObjectRepresentation( representation, typeToNode );
        }
        else
        {
            super.addToNodeSpace( representation );
        }
    }

    private void handleAddLiteralRepresentation(
        AbstractRepresentation representation,
        Map<String, AbstractNode> typeToNode )
    {
        AbstractNode abstractSubjectNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_SUBJECT );
        AbstractNode abstractMiddleNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_MIDDLE );
        NodeContext subjectNode =
        	lookupOrCreateNode( abstractSubjectNode, null );
        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_SUBJECT,
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToLiteral = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_MIDDLE,
            VerboseQuadStrategy.TYPE_LITERAL );
        AbstractNode abstractLiteralNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_LITERAL );

        Node middleNode = null;
        Node literalNode = null;
        if ( !subjectNode.wasCreated() )
        {
            Node[] nodes = findMiddleAndObjectNode( subjectNode.getNode(),
                subjectToMiddle, middleToLiteral, abstractLiteralNode, null );
            middleNode = nodes[ 0 ];
            literalNode = nodes[ 1 ];
        }
        boolean justAddContext = false;
        if ( literalNode == null )
        {
            justAddContext = true;
            middleNode = createNode( abstractMiddleNode, null );
            createRelationship( subjectNode.getNode(),
                subjectToMiddle, middleNode );
            incrementSubjectEnergy( subjectNode.getNode() );
            literalNode = createLiteralNode( abstractLiteralNode );
            createRelationship( middleNode, middleToLiteral, literalNode );
        }
        ensureContextsAreAdded( representation, middleNode, justAddContext );
    }
    
    private Map<String, AbstractNode> getTypeToNodeMap(
        AbstractRepresentation representation )
    {
        Map<String, AbstractNode> map = new HashMap<String, AbstractNode>();
        for ( AbstractNode node : representation.nodes() )
        {
            Collection<Object> nodeTypes = getNodeTypes( node );
            if ( nodeTypes != null )
            {
                for ( Object nodeType : nodeTypes )
                {
                    map.put( ( String ) nodeType, node );
                }
            }
        }
        return map;
    }

    private Relationship findContextRelationship(
        AbstractRelationship abstractRelationship, Node middleNode,
        boolean allowCreate, boolean justAddContext )
    {
        AbstractNode abstractContextNode = abstractRelationship.getEndNode();
        Node contextNode = lookupNode( abstractContextNode );
        if ( contextNode == null && !allowCreate )
        {
            return null;
        }
        
        boolean willCreateContextNode = contextNode == null;
        contextNode = contextNode != null ? contextNode :
            createNode( abstractContextNode, null );
        Relationship relationship = null;
        if ( willCreateContextNode )
        {
            Node contextRefNode = getContextsReferenceNode();
            contextRefNode.createRelationshipTo( contextNode,
                RelTypes.IS_A_CONTEXT );
            relationship = createRelationship( middleNode,
                abstractRelationship, contextNode );
            incrementContextCounter( contextNode );
            contextNode.setProperty( IS_CONTEXT_KEY, true );
        }
        else
        {
            if ( !justAddContext )
            {
                relationship = ensureDirectlyConnected( middleNode,
                    abstractRelationship, contextNode );
            }
            else
            {
                createRelationship( middleNode, abstractRelationship, 
                    contextNode );
            }
            if ( relationship == null )
            {
                // It means that it was created.
                incrementContextCounter( contextNode );
            }
            
            // use property optimization here to avoid load of all relationships
            // on a heavily connected context node
            if ( !contextNode.hasProperty( IS_CONTEXT_KEY ) )
            {
                if ( !contextNode.hasRelationship( RelTypes.IS_A_CONTEXT,
                    Direction.INCOMING ) )
                {
                    Node contextRefNode = getContextsReferenceNode();
                    contextRefNode.createRelationshipTo( contextNode,
                        RelTypes.IS_A_CONTEXT );
                    contextNode.setProperty( IS_CONTEXT_KEY, true );
                }
            }
        }
        return relationship;
    }
    
    private void incrementSubjectEnergy( Node node )
    {
//    	neoUtil().incrementAndGetCounter( node, SUBJECT_ENERGY );
    }

    private void decrementSubjectEnergy( Node node )
    {
//    	neoUtil().decrementAndGetCounter( node, SUBJECT_ENERGY, 0 );
    }
    
    private void incrementObjectEnergy( Node node )
    {
//    	neoUtil().incrementAndGetCounter( node, OBJECT_ENERGY );
    }

    private void decrementObjectEnergy( Node node )
    {
//    	neoUtil().decrementAndGetCounter( node, OBJECT_ENERGY, 0 );
    }
    
    private void incrementContextCounter( Node node )
    {
    	neoUtil().incrementAndGetCounter( node, STATEMENT_COUNT );
    }

    private void decrementContextCounter( Node contextNode )
    {
    	neoUtil().decrementAndGetCounter( contextNode, STATEMENT_COUNT, 0 );
    }
    
    private void handleAddObjectRepresentation(
        AbstractRepresentation representation,
        Map<String, AbstractNode> typeToNode )
    {
        AbstractNode abstractSubjectNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_SUBJECT );
        AbstractNode abstractMiddleNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractNode abstractObjectNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_OBJECT );
        NodeContext subjectNode =
            lookupOrCreateNode( abstractSubjectNode, null );
        NodeContext objectNode = lookupOrCreateNode( abstractObjectNode, null );
        AbstractRelationship subjectToMiddle = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_SUBJECT,
            VerboseQuadStrategy.TYPE_MIDDLE );
        AbstractRelationship middleToObject = findAbstractRelationship(
            representation, VerboseQuadStrategy.TYPE_MIDDLE,
            VerboseQuadStrategy.TYPE_OBJECT );
        Node middleNode = null;
        if ( !subjectNode.wasCreated() && !objectNode.wasCreated() )
        {
            middleNode = findMiddleAndObjectNode( subjectNode.getNode(),
                subjectToMiddle, middleToObject, abstractObjectNode,
                objectNode.getNode() )[ 0 ];
        }
        boolean justAddContext = false;
        if ( middleNode == null )
        {
            justAddContext = true;
            middleNode = createNode( abstractMiddleNode, null );
            createRelationship( subjectNode.getNode(), subjectToMiddle,
                middleNode );
            incrementSubjectEnergy( subjectNode.getNode() );
            createRelationship( middleNode, middleToObject,
                objectNode.getNode() );
            incrementObjectEnergy( objectNode.getNode() );
        }
        ensureContextsAreAdded( representation, middleNode, justAddContext );
    }
    
    private Node[] findMiddleAndObjectNode( Node subjectNode,
        AbstractRelationship subjectToMiddle,
        AbstractRelationship middleToObject,
        AbstractNode abstractObjectNode, Node objectNodeIfResource )
    {
        Node objectNodeToLookFor = null;
        if ( abstractObjectNode.getUriOrNull() != null )
        {
            objectNodeToLookFor = objectNodeIfResource;
        }

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
                        EXCLUDED_LITERAL_KEYS ) ) )
                {
                    return new Node[] { aMiddleNode, anObjectNode };
                }
            }
        }
        return new Node[] { null, null };
    }

    private void ensureContextsAreAdded(
        AbstractRepresentation representation, Node middleNode, 
        boolean justAddContext )
    {
        for ( AbstractRelationship abstractRelationship :
            getContextRelationships( representation ) )
        {
            findContextRelationship( abstractRelationship,
                middleNode, true, justAddContext );
        }
    }

    private Collection<AbstractRelationship> getContextRelationships(
        AbstractRepresentation representation )
    {
        Collection<AbstractRelationship> list =
            new ArrayList<AbstractRelationship>();
        for ( AbstractRelationship abstractRelationship :
            representation.relationships() )
        {
            String type = ( String ) abstractRelationship.getSingleExecutorInfo(
                VerboseQuadStrategy.EXECUTOR_INFO_NODE_TYPE );
            if ( type != null &&
                type.equals( VerboseQuadStrategy.TYPE_CONTEXT ) )
            {
                list.add( abstractRelationship );
            }
        }
        return list;
    }

    private boolean isLiteralRepresentation(
        Map<String, AbstractNode> typeToNode )
    {
        return typeToNode.get( VerboseQuadStrategy.TYPE_LITERAL ) != null;
    }

    private boolean isObjectTypeRepresentation(
        Map<String, AbstractNode> typeToNode )
    {
        return typeToNode.get( VerboseQuadStrategy.TYPE_OBJECT ) != null;
    }
    
    private Collection<Object> getNodeTypes( AbstractNode node )
    {
        return node.getExecutorInfo(
            VerboseQuadStrategy.EXECUTOR_INFO_NODE_TYPE );
    }

    private boolean nodeIsType( AbstractNode node, String type )
    {
        Collection<Object> nodeTypes = getNodeTypes( node );
        return nodeTypes != null && nodeTypes.contains( type );
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
        Map<String, AbstractNode> typeToNode =
            getTypeToNodeMap( representation );
        if ( isLiteralRepresentation( typeToNode ) )
        {
            handleRemoveLiteralRepresentation( representation, typeToNode );
        }
        else if ( isObjectTypeRepresentation( typeToNode ) )
        {
            handleRemoveObjectRepresentation( representation, typeToNode );
        }
        else
        {
            super.addToNodeSpace( representation );
        }
    }

    private void handleRemoveObjectRepresentation(
        AbstractRepresentation representation,
        Map<String, AbstractNode> typeToNode )
    {
        AbstractNode abstractSubjectNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_SUBJECT );
        Node subjectNode = lookupNode( abstractSubjectNode );
        AbstractNode abstractObjectNode = typeToNode.get(
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
            middleToObject, abstractObjectNode, objectNode )[ 0 ];
        if ( middleNode == null )
        {
            return;
        }

        removeContextRelationships( representation, middleNode );
        if ( !middleNodeHasContexts( middleNode ) )
        {
            disconnectMiddle( middleNode, middleToObject, objectNode,
                subjectNode, subjectToMiddle );
            decrementSubjectEnergy( subjectNode );
            decrementObjectEnergy( objectNode );
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
            Node contextNode = middleNode.getSingleRelationship(
                VerboseQuadStrategy.RelTypes.IN_CONTEXT,
                Direction.OUTGOING ).getEndNode();
            decrementContextCounter( contextNode );
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
                    decrementContextCounter( contextNode );
                    deleteRelationship( relationship );
                }
            }
        }
    }

    private void removeContextRelationships(
        AbstractRepresentation representation, Node middleNode )
    {
        Collection<AbstractRelationship> contextRelationships =
            getContextRelationships( representation );
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
        AbstractRepresentation representation,
        Map<String, AbstractNode> typeToNode )
    {
        AbstractNode abstractSubjectNode = typeToNode.get(
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
        AbstractNode abstractLiteralNode = typeToNode.get(
            VerboseQuadStrategy.TYPE_LITERAL );

        Node[] nodes = findMiddleAndObjectNode( subjectNode, subjectToMiddle,
            middleToLiteral, abstractLiteralNode, null );
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
            decrementSubjectEnergy( subjectNode );
        }
        deleteNodeIfEmpty( abstractSubjectNode, subjectNode );
    }

    private boolean middleNodeHasContexts( Node middleNode )
    {
        return getExistingContextRelationships(
            middleNode ).iterator().hasNext();
    }

//    private String guessPredicateKey( Iterable<String> keys )
//    {
//        for ( String key : keys )
//        {
//            if ( !EXCLUDED_LITERAL_KEYS.contains( key ) )
//            {
//                return key;
//            }
//        }
//        return null;
//    }

    private void deleteMiddleAndLiteral( Node middleNode,
        AbstractRelationship middleToLiteral, Node literalNode,
        Node subjectNode, AbstractRelationship subjectToMiddle )
    {
        disconnectMiddle( middleNode, middleToLiteral, literalNode,
            subjectNode, subjectToMiddle );
        String predicate = middleToLiteral.getRelationshipTypeName();
        Object value = literalNode.getProperty(
            AbstractUriBasedExecutor.LITERAL_VALUE_KEY );
        deleteLiteralNode( literalNode, predicate, value );
    }

    private void disconnectMiddle( Node middleNode,
        AbstractRelationship middleToOther, Node otherNode,
        Node subjectNode, AbstractRelationship subjectToMiddle )
    {
        ensureDirectlyDisconnected( middleNode, middleToOther, otherNode );
        ensureDirectlyDisconnected( subjectNode, subjectToMiddle, middleNode,
            Direction.INCOMING );
        deleteNode( middleNode, null );
    }
}
