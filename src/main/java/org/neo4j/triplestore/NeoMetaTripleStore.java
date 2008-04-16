package org.neo4j.triplestore;

import java.net.URI;
import java.text.ParseException;
import java.util.Collection;

import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.neometa.structure.PropertyRange;

/**
 * Uses the {@link MetaStructure} component for additional structure.
 */
public class NeoMetaTripleStore extends NeoTripleStore
{
	/**
	 * The URI which represents a type of an instance.
	 */
	public static final String RDF_TYPE_URI =
		"http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	
	private MetaStructure meta;
	
	/**
	 * @param meta the {@link MetaStructure}.
	 * @param model the {@link TripleModel}.
	 */
	public NeoMetaTripleStore( MetaStructure meta, TripleModel model )
	{
		super( meta.neo(), model );
		this.meta = meta;
	}
	
	protected MetaStructure meta()
	{
		return this.meta;
	}
	
	private Collection<MetaStructureClass> getClasses( Node instance )
	{
		return new InstanceClassesCollection( meta(), instance );
	}

	@Override
	protected Object datatypeObjectToRealObject( Node subjectNode,
		String predicate, String object )
	{
		MetaStructureProperty metaProperty =
			meta().getGlobalNamespace().getMetaProperty( predicate, false );
		if ( metaProperty == null )
		{
			throw new RuntimeException( "Unsupported predicate '" +
				predicate + "'" );
		}
		
		Collection<MetaStructureClass> classes = getClasses( subjectNode );
		PropertyRange range = meta().lookup( metaProperty,
			MetaStructure.LOOKUP_PROPERTY_RANGE, classes.toArray(
				new MetaStructureClass[ classes.size() ] ) );
		try
		{
			return range == null ? object :
				range.rdfLiteralToJavaObject( object );
		}
		catch ( ParseException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	protected Node getObjectNode( String predicate, URI objectUri )
	{
		Node result = null;
		if ( isRdfType( predicate ) )
		{
			MetaStructureClass metaClass = meta().getGlobalNamespace().
				getMetaClass( objectUri.toString(), false );
			if ( metaClass == null )
			{
				throw new RuntimeException( "Unsupported type '" +
					objectUri.toString() + "'" );
			}
			result = metaClass.node();
		}
		else
		{
			result = getNodeByUri( objectUri );
		}
		return result;
	}
	
	@Override
	protected boolean connectWithoutModel( Node subjectNode, String predicate,
		Node objectNode )
	{
		if ( !isRdfType( predicate ) )
		{
			return false;
		}
		MetaStructureClass metaClass = new MetaStructureClass( meta(),
			objectNode );
		metaClass.getInstances().add( subjectNode );
		return true;
	}
	
	@Override
	protected boolean disconnectWithoutModel( Node subjectNode,
		String predicate, Node objectNode )
	{
		if ( !isRdfType( predicate ) )
		{
			return false;
		}
		MetaStructureClass metaClass = new MetaStructureClass( meta(),
			objectNode );
		getClasses( subjectNode ).remove( metaClass );
		return true;
	}

	protected boolean isRdfType( String uri )
	{
		return uri.equals( RDF_TYPE_URI );
	}
}
