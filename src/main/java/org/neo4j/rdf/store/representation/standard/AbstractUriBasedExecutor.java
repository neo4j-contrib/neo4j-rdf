package org.neo4j.rdf.store.representation.standard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.transaction.LockManager;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.AbstractElement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;
import org.neo4j.util.GraphDatabaseUtil;
import org.neo4j.util.PropertyArraySet;

public abstract class AbstractUriBasedExecutor implements RepresentationExecutor
{
    public static final String START_OF_ILLEGAL_URI = "%";

    public static final int DEFAULT_URI_CACHE_SIZE = 100000;
    public static final String URI_PROPERTY_KEY = "uri";
    public static final String LITERAL_VALUE_KEY = "value";
    public static final String META_EXECUTOR_INFO_KEY = "meta";
    public static final String RDF_NAMESPACE =
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDF_TYPE_URI = RDF_NAMESPACE + "type";

    private final GraphDatabaseService graphDb;
    private final GraphDatabaseUtil graphDbUtil;
    private final IndexService index;
    private final MetaModel model;
    private FulltextIndex fulltextIndex;
    
    public AbstractUriBasedExecutor( GraphDatabaseService graphDb, IndexService index,
        MetaModel optionalModel, FulltextIndex optionalFulltextIndex )
    {
        this.graphDb = graphDb;
        this.index = index;
        this.graphDbUtil = new GraphDatabaseUtil( graphDb );
        this.model = optionalModel;
        this.fulltextIndex = optionalFulltextIndex;
        
        if ( this.index instanceof LuceneIndexService )
        {
            LuceneIndexService luceneIndex = (LuceneIndexService) this.index;
            Integer cacheSize = luceneIndex.getEnabledCacheSize( URI_PROPERTY_KEY );
            if ( cacheSize == null )
            {
                // TODO Really have System.out here?
                System.out.println( "Cache not enabled for '" + URI_PROPERTY_KEY +
                        "' Setting it (" + luceneIndex.getClass().getSimpleName() +
                        "#enableCache" + ") to " + DEFAULT_URI_CACHE_SIZE );
                luceneIndex.enableCache( URI_PROPERTY_KEY, DEFAULT_URI_CACHE_SIZE );
            }
        }
    }
    
    public FulltextIndex getFulltextIndex()
    {
        return this.fulltextIndex;
    }
    
    protected GraphDatabaseService graphDB()
    {
        return this.graphDb;
    }

    protected IndexService index()
    {
        return this.index;
    }

    protected GraphDatabaseUtil graphDbUtil()
    {
        return this.graphDbUtil;
    }

//    protected MetaStructure meta()
//    {
//        return this.meta;
//    }

//    protected void debug( String message )
//    {
//        System.out.println( message );
//    }
//
//    private void debugRelationship( Relationship relationship,
//        boolean create )
//    {
//        String sign = create ? "+" : "-";
//        debug( "\t" + sign + "Relationship (" + relationship + ") " +
//            relationship.getStartNode() + " --[" +
//            relationship.getType().name() + "]--> " +
//            relationship.getEndNode() );
//    }

//    protected void debugCreateRelationship( Relationship relationship )
//    {
//        debugRelationship( relationship, true );
//    }

//    protected void debugDeleteRelationship( Relationship relationship )
//    {
//        debugRelationship( relationship, false );
//    }

//    protected void debugCreateNode( Node node, String uri )
//    {
//        debug( "\t+Node (" + node.getId() + ") " + ( uri == null ? "" : uri ) );
//    }

//    protected void debugDeleteNode( Node node, String uri )
//    {
//        debug( "\t-Node (" + node.getId() + ") " + ( uri == null ? "" : uri ) );
//    }

    protected String getNodeUri( AbstractNode node )
    {
        Uri uri = node.getUriOrNull();
        return uri == null ? null : uri.getUriAsString();
    }

    public Node lookupNode( AbstractNode abstractNode )
    {
        Node result = null;
//        if ( isMeta( abstractNode ) )
//        {
//            MetaStructureThing thing = getMetaStructureThing( abstractNode );
//            result = thing == null ? null : thing.node();
//        }
//        else
//        {
            result = index().getSingleNode( URI_PROPERTY_KEY,
                getNodeUri( abstractNode ) );
//        }
        return result;
    }

    protected NodeContext lookupOrCreateNode( AbstractNode abstractNode,
    	Map<AbstractNode, Node> nodeMapping )
    {
        Node node = lookupNode( abstractNode );
        boolean created = false;
        if ( node == null )
        {
            node = createNode( abstractNode, nodeMapping );
            created = true;
        }
        else
        {
            if ( nodeMapping != null )
            {
                nodeMapping.put( abstractNode, node );
            }
        }
        return new NodeContext( node, created );
    }

    protected void deleteNode( Node node, Uri uriOrNull )
    {
//        debugDeleteNode( node, uriOrNull == null ? null :
//            uriOrNull.getUriAsString() );
        node.delete();
        if ( uriOrNull != null )
        {
            index().removeIndex( node, URI_PROPERTY_KEY,
                uriOrNull.getUriAsString() );
        }
    }

    protected void deleteNodeIfEmpty( AbstractNode abstractNode, Node node )
    {
        if ( nodeIsEmpty( abstractNode, node, true ) )
        {
            deleteNode( node, abstractNode.getUriOrNull() );
        }
    }

//    protected MetaStructureThing getMetaStructureThing( AbstractNode node )
//    {
//        MetaStructureThing thing = null;
//        String metaInfo = getMetaExecutorInfo( node );
//        if ( node.getUriOrNull() == null )
//        {
//        }
//        else if ( metaInfo.equals( "class" ) )
//        {
//            thing = meta().getGlobalNamespace().getMetaClass(
//                node.getUriOrNull().getUriAsString(), false );
//        }
//        else if ( metaInfo.equals( "property" ) )
//        {
//            thing = meta().getGlobalNamespace().getMetaProperty(
//                node.getUriOrNull().getUriAsString(), false );
//        }
//        else
//        {
//            throw new IllegalArgumentException( "Strange meta info '" +
//                metaInfo + "'" );
//        }
//        return thing;
//    }

    public String getNodeUriPropertyKey( AbstractNode abstractNode )
    {
//        return isMeta( abstractNode ) ? MetaStructureObject.KEY_NAME :
//            URI_PROPERTY_KEY;
        return URI_PROPERTY_KEY;
    }

    protected Relationship findDirectRelationship( Node startNode,
        RelationshipType relType, Node endNode, Direction directionOrNull )
    {
        Relationship relationship = null;
        Iterable<Relationship> relationships = directionOrNull == null ?
            startNode.getRelationships( relType ) :
                startNode.getRelationships( relType, directionOrNull );
        for ( Relationship rel : relationships )
        {
            if ( rel.getOtherNode( startNode ).equals( endNode ) )
            {
                relationship = rel;
                break;
            }
        }
        return relationship;
    }

    protected void ensureDirectlyDisconnected( Node startNode,
        AbstractRelationship abstractRelationship, Node endNode )
    {
        ensureDirectlyDisconnected( startNode, abstractRelationship, endNode,
            Direction.OUTGOING );
    }
    
    protected void ensureDirectlyDisconnected( Node startNode,
        AbstractRelationship abstractRelationship, Node endNode,
        Direction optimizeDirection )
    {
        Node start = optimizeDirection == Direction.OUTGOING ?
            startNode : endNode;
        Node end = optimizeDirection == Direction.OUTGOING ?
            endNode : startNode;
        
        RelationshipType relType = relationshipType(
            abstractRelationship.getRelationshipTypeName() );
        Relationship relationship = findDirectRelationship( start, relType,
            end, optimizeDirection );
        if ( relationship != null )
        {
            deleteRelationship( relationship );
        }
    }

    protected Relationship ensureDirectlyConnected( Node startNode,
        AbstractRelationship abstractRelationship, Node endNode )
    {
        return ensureDirectlyConnected( startNode, abstractRelationship,
            endNode, Direction.OUTGOING );
    }
    
    protected Relationship ensureDirectlyConnected( Node startNode,
        AbstractRelationship abstractRelationship, Node endNode,
        Direction optimizeDirection )
    {
        Node start = optimizeDirection == Direction.OUTGOING ?
            startNode : endNode;
        Node end = optimizeDirection == Direction.OUTGOING ?
            endNode : startNode;
        RelationshipType relType = relationshipType(
            abstractRelationship.getRelationshipTypeName() );
        Relationship relationship = findDirectRelationship(
            start, relType, end, optimizeDirection );
        if ( relationship == null )
        {
            createRelationship( start, abstractRelationship, end );
        }
        return relationship;
    }

    protected RelationshipType relationshipType( String name )
    {
        return DynamicRelationshipType.withName( name );
    }

    protected String getMetaExecutorInfo( AbstractNode node )
    {
        return ( String ) node.getSingleExecutorInfo( META_EXECUTOR_INFO_KEY );
    }

//    protected boolean isMeta( AbstractNode node )
//    {
//        return meta() != null && getMetaExecutorInfo( node ) != null;
//    }

    protected boolean containsProperties( PropertyContainer container,
        Map<String, Collection<Object>> containingProperties,
        Collection<String> excludeThese )
    {
        for ( Map.Entry<String, Collection<Object>> entry :
            containingProperties.entrySet() )
        {
            String key = entry.getKey();
            if ( excludeThese.contains( key ) )
            {
                continue;
            }
            Collection<Object> values = entry.getValue();
            Collection<Object> rawValues =
                graphDbUtil().getPropertyValues( container, key );
            if ( !rawValues.containsAll( values ) )
            {
                return false;
            }
        }
        return true;
    }

    protected Map<AbstractNode, Node> getWellKnownNodeMappings(
        AbstractRepresentation representation )
    {
        Map<AbstractNode, Node> nodeMapping = new HashMap<AbstractNode, Node>();
        for ( AbstractNode abstractNode : representation.nodes() )
        {
            if ( abstractNode.getUriOrNull() != null )
            {
                Node node = lookupNode( abstractNode );
                if ( node == null )
                {
                    return null;
                }
                nodeMapping.put( abstractNode, node );
            }
        }
        return nodeMapping;
    }

    protected boolean applyRepresentation( AbstractElement abstractElement,
        PropertyContainer container )
    {
        boolean changed = false;
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractElement.properties().entrySet() )
        {
            Collection<Object> rawValues = new PropertyArraySet<Object>(
                graphDB(), container, entry.getKey() );
            for ( Object value : entry.getValue() )
            {
                boolean added = rawValues.add( value );
                if ( added )
                {
                    changed = true;
//                    debug( "\t+Property" + " (" + container + ") "
//                        + entry.getKey() + " " + "[" + value + "]" );
                }
            }
        }
        return changed;
    }

    protected boolean removeRepresentation( AbstractElement abstractElement,
        PropertyContainer container )
    {
        boolean changed = false;
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractElement.properties().entrySet() )
        {
            Collection<Object> rawValues = new PropertyArraySet<Object>(
                graphDB(), container, entry.getKey() );
            for ( Object value : entry.getValue() )
            {
                boolean removed = rawValues.remove( value );
                if ( removed )
                {
                    changed = true;
//                    debug( "\t-Property" + " (" + container + ") "
//                        + entry.getKey() + " " + "[" + value + "]" );
                }
            }
        }
        return changed;
    }

    protected boolean nodeIsEmpty( AbstractNode abstractNode, Node node,
        boolean checkRelationships )
    {
        if ( checkRelationships && node.hasRelationship() )
        {
            return false;
        }
        String legalKey = getNodeUriPropertyKey( abstractNode );
        for ( String key : node.getPropertyKeys() )
        {
            if ( !key.equals( legalKey ) )
            {
                return false;
            }
        }
        return true;
    }

    protected boolean removeRepresentation( AbstractElement element,
        PropertyContainer container, String key )
    {
        boolean someRemoved = false;
        Collection<Object> rawValues = new PropertyArraySet<Object>(
            graphDB(), container, key );
        for ( Object value : element.properties().get( key ) )
        {
            boolean removed = rawValues.remove( value );
            if ( removed )
            {
                someRemoved = true;
//                debug( "\t-Property (" + container + ") "
//                    + key + " " + "[" + value + "]" );
            }
        }
        return someRemoved;
    }

    protected Object getRealLiteralValue( Literal literal )
    {
    	// TODO Conversion?
        return literal.getValue();
    }

    protected void indexLiteral( Node node, Uri predicate, Object literalValue )
    {
        index().index( node, LITERAL_VALUE_KEY, literalValue );
        if ( getFulltextIndex() != null )
        {
            getFulltextIndex().index( node, predicate, literalValue );
        }
    }
    
    protected void removeLiteralIndex( Node node, Uri predicate,
        Object literalValue )
    {
        index().removeIndex( node, LITERAL_VALUE_KEY, literalValue );
        if ( getFulltextIndex() != null )
        {
            getFulltextIndex().removeIndex( node, predicate, literalValue );
        }
    }

    public Iterable<Node> findLiteralNodes( Object value )
    {
        return index().getNodes( LITERAL_VALUE_KEY, value );
    }

    protected Relationship createRelationship( Node from,
        AbstractRelationship abstractRelationship, Node to )
    {
        Relationship relationship = from.createRelationshipTo( to,
            relationshipType( abstractRelationship.
                getRelationshipTypeName() ) );
        applyRepresentation( abstractRelationship, relationship );
//        debugCreateRelationship( relationship );
        return relationship;
    }

    protected Node createNode( AbstractNode abstractNode,
    	Map<AbstractNode, Node> nodeMapping )
    {
        Node node = graphDb.createNode();
        Uri uri = abstractNode.getUriOrNull();
        if ( uri != null )
        {
            node.setProperty( URI_PROPERTY_KEY, uri.getUriAsString() );
            index().index( node, URI_PROPERTY_KEY, uri.getUriAsString() );
        }
        applyRepresentation( abstractNode, node );
//        debugCreateNode( node, uri == null ? null : uri.toString() );
        if ( nodeMapping != null )
        {
            nodeMapping.put( abstractNode, node );
        }
        return node;
    }

    protected void deleteRelationship( Relationship relationship )
    {
        getLockManager().getReadLock( relationship );
        try
        {
            relationship.delete();
        }
        catch ( NotFoundException e )
        {
            // ok some other thread deleted it
        }
        finally
        {
            getLockManager().releaseReadLock( relationship );
        }
    }
    
    private LockManager getLockManager()
    {
        return ((EmbeddedGraphDatabase) graphDb).getConfig().getLockManager();
    }
    
    protected static class NodeContext
    {
        private Node node;
        private boolean created;
        
        protected NodeContext( Node node, boolean created )
        {
            this.node = node;
            this.created = created;
        }
        
        protected Node getNode()
        {
            return this.node;
        }
        
        protected boolean wasCreated()
        {
            return this.created;
        }
    }
}
