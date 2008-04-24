package org.neo4j.rdf.store.representation.standard;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.neometa.structure.MetaStructureRelTypes;
import org.neo4j.neometa.structure.PropertyRange;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;
import org.neo4j.rdf.store.representation.RepresentationStrategy;
import org.neo4j.util.NeoUtil;
import org.neo4j.util.index.Index;
import org.neo4j.util.index.SingleValueIndex;

/**
 * Abstract class which holds common functionality for
 * {@link RepresentationStrategy} implementations using an
 * {@link UriBasedExecutor}.
 */
abstract class IndexRepresentationStrategy implements RepresentationStrategy
{
    /**
     * The property postfix which is concatenated with a property key to get
     * the context property key on a node (literal values).
     */
    public static final String CONTEXT_PROPERTY_POSTFIX = "____context";
    
    private final RepresentationExecutor executor;
    private final MetaStructure meta;

    /**
     * @param neo the {@link NeoService}.
     */
    public IndexRepresentationStrategy( NeoService neo )
    {
        this.executor = new UriBasedExecutor( neo, newIndex( neo ) );
        this.meta = null;
    }

    /**
     * @param neo the {@link NeoService}.
     * @param meta the {@link MetaStructure}.
     */
    public IndexRepresentationStrategy( NeoService neo, MetaStructure meta )
    {
        this.executor =
            new MetaEnabledUriBasedExecutor( neo, newIndex( neo ), meta );
        this.meta = meta;
    }

    private static Index newIndex( NeoService neo )
    {
        Node indexNode = new NeoUtil( neo )
            .getOrCreateSubReferenceNode( MyRelTypes.INDEX_ROOT );
        return new SingleValueIndex( "blaaaa", indexNode, neo );
    }

    public AbstractRepresentation getAbstractRepresentation(
        Statement... statements )
    {
        AbstractRepresentation representation =
            new AbstractRepresentation();
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
        AbstractRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        String predicate =
            ( ( Uri ) statement.getPredicate() ).getUriAsString();
        if ( predicate.equals( MetaEnabledUriBasedExecutor.RDF_TYPE_URI ) )
        {
            addMetaInstanceOfFragment( representation, nodeMapping, statement );
            return true;
        }
        return false;
    }
    
    public RepresentationExecutor getExecutor()
    {
        return this.executor;
    }
    
    protected void addMetaInstanceOfFragment(
        AbstractRepresentation representation,
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
        AbstractRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode subjectNode = getSubjectNode( nodeMapping, statement );
        Value object = statement.getObject();
        Object literalValue = null;
        String predicate =
            ( ( Uri ) statement.getPredicate() ).getUriAsString();
        if ( object instanceof Wildcard )
        {
        	subjectNode.addProperty( predicate, ( Wildcard ) object );
        	return;
        }
        else
        {
        	literalValue = ( ( Literal ) statement.getObject() ).getValue();
        }
        
        // TODO Should this be here?
        if ( literalValue != null )
        {
            convertLiteralValueToRealValue( statement, literalValue );
        }
        
        subjectNode.addProperty( predicate, literalValue );
        String predicateContext = UriBasedExecutor.formContextPropertyKey(
            predicate, literalValue );
        for ( Context context : statement.getContexts() )
        {
            subjectNode.addProperty( predicateContext,
                context.getUriAsString() );
        }
        Map<String, String> contextKeys = new HashMap<String, String>();
        contextKeys.put( predicateContext, predicate );
        subjectNode.addLookupInfo( UriBasedExecutor.LOOKUP_CONTEXT_KEYS,
            contextKeys );
    }
    
    private PropertyRange getPropertyRange( String predicate )
    {
        if ( meta == null )
        {
            return null;
        }

        MetaStructureProperty property =
            meta.getGlobalNamespace().getMetaProperty( predicate, false );
        return property == null ? null :
            meta.lookup( property, MetaStructure.LOOKUP_PROPERTY_RANGE );
    }
    
    protected boolean isObjectType( String predicate )
    {
        PropertyRange range = getPropertyRange( predicate );
        if ( range == null )
        {
        	return false;
//            throw new UnsupportedOperationException( "No range found for '" +
//                predicate + "'" );
        }
        return !range.isDatatype();
    }
    
    protected Object convertLiteralValueToRealValue( Statement statement,
        Object literalValue )
    {
        Object result = literalValue;
        if ( result != null && result instanceof String && meta != null )
        {
            PropertyRange range = getPropertyRange(
                ( ( Uri ) statement.getPredicate() ).getUriAsString() );
            if ( range != null && range.isDatatype() )
            {
                try
                {
                    result = range.rdfLiteralToJavaObject(
                        literalValue.toString() );
                }
                catch ( ParseException e )
                {
                    // Ok?
                }
            }
        }
        return result;
    }
    
    protected AbstractNode getOrCreateNode(
        Map<String, AbstractNode> nodeMapping, Value value )
    {
        AbstractNode node = nodeMapping.get( this.asString( value ) );
        if ( node == null )
        {
            node = new AbstractNode( value );
            nodeMapping.put( this.asString( value ), node );
        }
        return node;
    }
    
    protected String asUri( Value value )
    {
        return ( ( Uri ) value ).getUriAsString();
    }
    
    protected String asString( Value value )
    {
    	String string = null;
    	if ( value instanceof Wildcard )
    	{
    		string = ( ( Wildcard ) value ).getVariableName(); 
    	}
    	else if ( value instanceof Uri )
    	{
    		string = ( ( Uri ) value ).getUriAsString();
    	}
    	else if ( value instanceof Literal )
    	{
    		string = ( ( Literal ) value ).getValue().toString();
    	}
    	return string;
    }

    protected AbstractNode getSubjectNode(
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
       return getOrCreateNode( nodeMapping, statement.getSubject() );
    }

    protected AbstractNode getObjectNode( Map<String, AbstractNode> nodeMapping,
        Statement statement )
    {
        return getOrCreateNode( nodeMapping, statement.getObject() );
    }
    
    protected AbstractNode getPredicateNode(
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode node = getOrCreateNode( nodeMapping,
        		statement.getPredicate() );
        node.addLookupInfo( "meta", "property" );
        return node;
    }
    
    protected boolean isObjectType( Value value )
    {
        return value instanceof Resource;
    }
    
    protected void addContextsToRelationship( Statement statement,
        AbstractRelationship relationship )
    {
        for ( Context context : statement.getContexts() )
        {
            relationship.addProperty( CONTEXT_PROPERTY_POSTFIX,
                context.getUriAsString() );
        }
        Map<String, String> contextKeys = new HashMap<String, String>();
        contextKeys.put( CONTEXT_PROPERTY_POSTFIX, null );
        relationship.addLookupInfo( UriBasedExecutor.LOOKUP_CONTEXT_KEYS,
            contextKeys );
    }

    private static enum MyRelTypes implements RelationshipType
    {
        /**
         * Neo reference node --> Uri index node.
         */
        INDEX_ROOT,
    }
}
