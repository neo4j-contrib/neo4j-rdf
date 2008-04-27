package org.neo4j.rdf.store.representation.standard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.PropertyContainer;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureObject;
import org.neo4j.neometa.structure.MetaStructureThing;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.AbstractElement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;
import org.neo4j.util.NeoPropertyArraySet;
import org.neo4j.util.NeoUtil;
import org.neo4j.util.index.IndexService;

public abstract class AbstractUriBasedExecutor implements RepresentationExecutor
{
    public static final String URI_PROPERTY_KEY = "uri";
    public static final String LITERAL_VALUE_KEY = "value";
    public static final String META_EXECUTOR_INFO_KEY = "meta";
    public static final String RDF_NAMESPACE =
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDF_TYPE_URI = RDF_NAMESPACE + "type";

    private final NeoService neo;
    private final NeoUtil neoUtil;
    private final IndexService index;
    private final MetaStructure meta;

    public AbstractUriBasedExecutor( NeoService neo, IndexService index,
        MetaStructure optionalMeta )
    {
        this.neo = neo;
        this.index = index;
        this.neoUtil = new NeoUtil( neo );
        this.meta = optionalMeta;
    }

    protected NeoService neo()
    {
        return this.neo;
    }

    protected IndexService index()
    {
        return this.index;
    }

    protected NeoUtil neoUtil()
    {
        return this.neoUtil;
    }

    protected MetaStructure meta()
    {
        return this.meta;
    }

    protected void debug( String message )
    {
        System.out.println( message );
    }

    private void debugRelationship( Relationship relationship,
        boolean create )
    {
        String sign = create ? "+" : "-";
        debug( "\t" + sign + "Relationship (" + relationship + ") " +
            relationship.getStartNode() + " --[" +
            relationship.getType().name() + "]--> " +
            relationship.getEndNode() );
    }

    protected void debugCreateRelationship( Relationship relationship )
    {
        debugRelationship( relationship, true );
    }

    protected void debugRemoveRelationship( Relationship relationship )
    {
        debugRelationship( relationship, false );
    }

    protected void debugCreateNode( Node node, String uri )
    {
        debug( "\t+Node (" + node.getId() + ") '" + uri + "'" );
    }

    protected void debugDeleteNode( Node node, String uri )
    {
        debug( "\t-Node (" + node.getId() + ") '" + uri + "'" );
    }

    protected String getNodeUri( AbstractNode node )
    {
        Uri uri = node.getUriOrNull();
        return uri == null ? "null" : uri.getUriAsString();
    }

    public Node lookupNode( AbstractNode abstractNode )
    {
        Node result = null;
        if ( isMeta( abstractNode ) )
        {
            MetaStructureThing thing = getMetaStructureThing( abstractNode );
            result = thing == null ? null : thing.node();
        }
        else
        {
            result = index().getSingleNode( URI_PROPERTY_KEY,
                getNodeUri( abstractNode ) );
        }
        return result;
    }

    protected Node lookupOrCreateNode( AbstractNode abstractNode,
        Map<AbstractNode, Node> abstractNodeToNodeMap )
    {
        Node node = lookupNode( abstractNode );
        if ( node == null )
        {
            String uri = getNodeUri( abstractNode );
            node = neo().createNode();
            node.setProperty( URI_PROPERTY_KEY, uri );
            index.index( node, URI_PROPERTY_KEY, uri );
            debugCreateNode( node, uri );
        }
        if ( abstractNodeToNodeMap != null )
        {
            abstractNodeToNodeMap.put( abstractNode, node );
        }
        return node;
    }

    protected void deleteNode( Node node, Uri uriOrNull )
    {
        debugDeleteNode( node, uriOrNull == null ? null :
            uriOrNull.getUriAsString() );
        node.delete();
        if ( uriOrNull != null )
        {
            index().removeIndex( node, URI_PROPERTY_KEY,
                uriOrNull.getUriAsString() );
        }
    }

    protected MetaStructureThing getMetaStructureThing( AbstractNode node )
    {
        MetaStructureThing thing = null;
        String metaInfo = getMetaExecutorInfo( node );
        if ( node.getUriOrNull() == null )
        {
            thing = null;
        }
        else if ( metaInfo.equals( "class" ) )
        {
            thing = meta().getGlobalNamespace().getMetaClass(
                node.getUriOrNull().getUriAsString(), false );
        }
        else if ( metaInfo.equals( "property" ) )
        {
            thing = meta().getGlobalNamespace().getMetaProperty(
                node.getUriOrNull().getUriAsString(), false );
        }
        else
        {
            throw new IllegalArgumentException( "Strange meta info '" +
                metaInfo + "'" );
        }
        return thing;
    }

    public String getNodeUriPropertyKey( AbstractNode abstractNode )
    {
        return isMeta( abstractNode ) ? MetaStructureObject.KEY_NAME :
            URI_PROPERTY_KEY;
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
        RelationshipType relType, Node endNode )
    {
        Relationship relationship = findDirectRelationship( startNode, relType,
            endNode, Direction.OUTGOING );
        if ( relationship != null )
        {
            debugRemoveRelationship( relationship );
            relationship.delete();
        }
    }

    protected Relationship ensureDirectlyConnected( Node startNode,
        RelationshipType relType, Node endNode )
    {
        Relationship relationship = findDirectRelationship(
            startNode, relType, endNode, Direction.OUTGOING );
        if ( relationship == null )
        {
            relationship = startNode.createRelationshipTo( endNode, relType );
            debugCreateRelationship( relationship );
        }
        return relationship;
    }

    protected RelationshipType relationshipType( String name )
    {
        return new ARelationshipType( name );
    }

    protected String getMetaExecutorInfo( AbstractNode node )
    {
        return ( String ) node.getExecutorInfo( META_EXECUTOR_INFO_KEY );
    }

    protected boolean isMeta( AbstractNode node )
    {
        return meta() != null && getMetaExecutorInfo( node ) != null;
    }

    protected boolean containsProperties( PropertyContainer container,
        Map<String, Collection<Object>> containingProperties )
    {
        for ( Map.Entry<String, Collection<Object>> entry :
            containingProperties.entrySet() )
        {
            String key = entry.getKey();
            Collection<Object> values = entry.getValue();
            Collection<Object> neoValues =
                neoUtil().getPropertyValues( container, key );
            if ( !neoValues.containsAll( values ) )
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
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo(), container, entry.getKey() );
            for ( Object value : entry.getValue() )
            {
                boolean added = neoValues.add( value );
                if ( added )
                {
                    changed = true;
                    debug( "\t+Property" + " (" + container + ") "
                        + entry.getKey() + " " + "[" + value + "]" );
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
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo(), container, entry.getKey() );
            for ( Object value : entry.getValue() )
            {
                boolean removed = neoValues.remove( value );
                if ( removed )
                {
                    changed = true;
                    debug( "\t-Property" + " (" + container + ") "
                        + entry.getKey() + " " + "[" + value + "]" );
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
        for ( String key : node.getPropertyKeys() )
        {
            if ( !key.equals( getNodeUriPropertyKey( abstractNode ) ) )
            {
                return false;
            }
        }
        return true;
    }

    protected boolean contextRelationshipIsEmpty(
        AbstractRelationship abstractRelationship, Relationship relationship )
    {
        return !relationship.hasProperty(
            StandardAbstractRepresentationStrategy.CONTEXT_PROPERTY_POSTFIX );
    }

    protected boolean removeRepresentation( AbstractElement element,
        PropertyContainer container, String key )
    {
        boolean someRemoved = false;
        Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
            neo(), container, key );
        for ( Object value : element.properties().get( key ) )
        {
            boolean removed = neoValues.remove( value );
            if ( removed )
            {
                someRemoved = true;
                debug( "\t-Property (" + container + ") "
                    + key + " " + "[" + value + "]" );
            }
        }
        return someRemoved;
    }

    protected Node createLiteralNode( String predicate, Object value )
    {
        Node node = neo.createNode();
        debugCreateNode( node, predicate );
        index().index( node, LITERAL_VALUE_KEY, value );
        return node;
    }

    protected void deleteLiteralNode( Node node,
        String predicate, Object value )
    {
        index().removeIndex( node, LITERAL_VALUE_KEY, value );
        deleteNode( node, null );
    }

    public Iterable<Node> findLiteralNodes( Object value )
    {
        return index().getNodes( LITERAL_VALUE_KEY, value );
    }

    private static class ARelationshipType implements RelationshipType
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
}
