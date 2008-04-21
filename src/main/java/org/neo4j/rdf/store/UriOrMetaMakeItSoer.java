package org.neo4j.rdf.store;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureObject;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.util.index.Index;

public class UriOrMetaMakeItSoer extends UriMakeItSoer
{
	private final MetaStructure meta;
	
	public UriOrMetaMakeItSoer( NeoService neo, Index index,
		MetaStructure meta )
	{
		super( neo, index );
		this.meta = meta;
	}
	
	private boolean isMeta( AbstractNode node )
	{
		return node.lookupInfo( "meta" ) != null;
	}

	@Override
    public Node lookupNode( AbstractNode node )
    {
		Node result = null;
		if ( isMeta( node ) )
		{
			result = meta.getGlobalNamespace().getMetaClass(
				node.getUriOrNull().uriAsString(), false ).node();
		}
		else
		{
			result = super.lookupNode( node );
		}
		return result;
    }

	@Override
	protected String getNodeUriProperty( AbstractNode node )
	{
		return isMeta( node ) ? MetaStructureObject.KEY_NAME : URI_PROPERTY_KEY;
	}
}
