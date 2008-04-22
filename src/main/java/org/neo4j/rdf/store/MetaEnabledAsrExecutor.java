package org.neo4j.rdf.store;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureObject;
import org.neo4j.neometa.structure.MetaStructureThing;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.util.index.Index;

/**
 * Adds meta model suport to the {@link UriAsrExecutor}.
 */
public class MetaEnabledAsrExecutor extends UriAsrExecutor
{
    /**
     * The URI which represents a type of an instance.
     */
    public static final String RDF_TYPE_URI =
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    
    /**
     * The lookup info key for meta enabled nodes.
     */
    public static final String META_LOOKUP_KEY = "meta";

    private final MetaStructure meta;
	
	/**
	 * @param neo the {@link NeoService}.
	 * @param index the {@link Index} to use as an object lookup.
	 * @param meta the {@link MetaStructure} to use as a class/property lookup.
	 */
	public MetaEnabledAsrExecutor( NeoService neo, Index index,
		MetaStructure meta )
	{
		super( neo, index );
		this.meta = meta;
	}
	
	private String getMetaLookupInfo( AbstractNode node )
	{
		return node.lookupInfo( "meta" );
	}
	
	private boolean isMeta( AbstractNode node )
	{
		return getMetaLookupInfo( node ) != null;
	}

	@Override
    protected Node lookupNode( AbstractNode node,
        boolean createIfItDoesntExist )
    {
		Node result = null;
		if ( isMeta( node ) )
		{
		    result = getMetaStructureThing( node ).node();
		}
		else
		{
			result = super.lookupNode( node, createIfItDoesntExist );
		}
		return result;
    }
	
	private MetaStructureThing getMetaStructureThing( AbstractNode node )
	{
	    MetaStructureThing thing = null;
	    String metaInfo = getMetaLookupInfo( node );
        if ( metaInfo.equals( "class" ) )
        {
            thing = meta.getGlobalNamespace().getMetaClass(
                node.getUriOrNull().uriAsString(), false );
        }
        else if ( metaInfo.equals( "property" ) )
        {
            thing = meta.getGlobalNamespace().getMetaProperty(
                node.getUriOrNull().uriAsString(), false );
        }
        else
        {
            throw new IllegalArgumentException( "Strange meta info '" +
                metaInfo + "'" );
        }
        return thing;
	}

	@Override
	protected String getNodeUriProperty( AbstractNode node )
	{
		return isMeta( node ) ? MetaStructureObject.KEY_NAME : URI_PROPERTY_KEY;
	}
	
	@Override
	public Node lookupNode( AbstractNode abstractNode )
	{
	    return lookupNode( abstractNode, false );
	}
}
