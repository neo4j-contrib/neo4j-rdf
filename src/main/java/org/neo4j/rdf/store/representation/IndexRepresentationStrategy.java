package org.neo4j.rdf.store.representation;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureRelTypes;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.MetaEnabledAsrExecutor;
import org.neo4j.rdf.store.UriAsrExecutor;
import org.neo4j.util.NeoUtil;
import org.neo4j.util.index.Index;
import org.neo4j.util.index.SingleValueIndex;

/**
 * Abstract class which holds common functionality for
 * {@link RdfRepresentationStrategy} implementations using an
 * {@link UriAsrExecutor}.
 */
abstract class IndexRepresentationStrategy implements
    RdfRepresentationStrategy
{
    private final AsrExecutor executor;
    private final MetaStructure meta;

    /**
     * @param neo the {@link NeoService}.
     */
    public IndexRepresentationStrategy( NeoService neo )
    {
        this.executor = new UriAsrExecutor( neo, newIndex( neo ) );
        this.meta = null;
    }

    /**
     * @param neo the {@link NeoService}.
     * @param meta the {@link MetaStructure}.
     */
    public IndexRepresentationStrategy( NeoService neo, MetaStructure meta )
    {
        this.executor =
            new MetaEnabledAsrExecutor( neo, newIndex( neo ), meta );
        this.meta = meta;
    }

    private static Index newIndex( NeoService neo )
    {
        Node indexNode = new NeoUtil( neo )
            .getOrCreateSubReferenceNode( MyRelTypes.INDEX_ROOT );
        return new SingleValueIndex( "blaaaa", indexNode, neo );
    }

    public AbstractStatementRepresentation getAbstractRepresentation(
        Statement... statements )
    {
        assert statements.length > 0;
        AbstractStatementRepresentation representation =
            new AbstractStatementRepresentation();
        Map<String, AbstractNode> nodeMapping =
            new HashMap<String, AbstractNode>();
        for ( Statement statement : statements )
        {
            if ( !addToRepresentation( representation, nodeMapping,
                statement ) )
            {
                throw new RuntimeException( "Implementation error" );
            }
        }
        for ( AbstractNode node : nodeMapping.values() )
        {
            representation.addNode( node );
        }
        return representation;
    }
    
    protected boolean addToRepresentation(
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        String predicate = statement.getPredicate().uriAsString();
        if ( predicate.equals( MetaEnabledAsrExecutor.RDF_TYPE_URI ) )
        {
            addMetaInstanceOfFragment( representation, nodeMapping, statement );
            return true;
        }
        return false;
    }
    
    public AsrExecutor getAsrExecutor()
    {
        return this.executor;
    }
    
    protected void addMetaInstanceOfFragment(
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode subjectNode = getSubjectNode( nodeMapping, statement );
        AbstractNode classNode = getObjectNode( nodeMapping, statement );
        classNode.addLookupInfo( "meta", "class" );
        AbstractRelationship instanceOfRelationship = new AbstractRelationship(
            subjectNode, MetaStructureRelTypes.META_IS_INSTANCE_OF.name(),
            classNode );
        representation.addRelationship( instanceOfRelationship );
    }

    protected void addOneNodeFragment(
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode subjectNode = getSubjectNode( nodeMapping, statement );
        Object literalValue = statement.getObject().getLiteralValueOrNull();
        
        // TODO Should this be here?
        if ( literalValue != null && meta != null )
        {
            convertLiteralValueToRealValue( literalValue );
        }
        
        subjectNode.addProperty( statement.getPredicate().uriAsString(),
            literalValue );
    }
    
    protected Object convertLiteralValueToRealValue( Object literalValue )
    {
        return literalValue;
    }
    
    protected AbstractNode getOrCreateNode(
        Map<String, AbstractNode> nodeMapping, String uri )
    {
        AbstractNode node = nodeMapping.get( uri );
        if ( node == null )
        {
            node = new AbstractNode( uri );
            nodeMapping.put( uri, node );
        }
        return node;
    }

    protected AbstractNode getSubjectNode(
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
       return getOrCreateNode( nodeMapping,
           statement.getSubject().uriAsString() );
    }

    protected AbstractNode getObjectNode( Map<String, AbstractNode> nodeMapping,
        Statement statement )
    {
        return getOrCreateNode( nodeMapping, 
            statement.getObject().getResourceOrNull().uriAsString() );
    }
    
    protected AbstractNode getPredicateNode(
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode node = getOrCreateNode( nodeMapping,
            statement.getPredicate().uriAsString() );
        node.addLookupInfo( "meta", "property" );
        return node;
    }

    private static enum MyRelTypes implements RelationshipType
    {
        /**
         * Neo reference node --> Uri index node.
         */
        INDEX_ROOT,
    }
}
