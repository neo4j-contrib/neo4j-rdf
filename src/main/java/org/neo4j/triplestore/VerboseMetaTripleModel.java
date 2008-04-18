package org.neo4j.triplestore;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureProperty;

/**
 * Uses a {@link MetaStructure} to find the predicate node.
 */
public class VerboseMetaTripleModel extends VerboseTripleModel
{
	private MetaStructure meta;
	
	/**
	 * @param neo the {@link NeoService}.
	 * @param meta the {@link MetaStructure}.
	 */
	public VerboseMetaTripleModel( NeoService neo, MetaStructure meta )
	{
		super( neo );
		this.meta = meta;
	}
	
	@Override
	protected Node getPredicateNode( String predicate )
	{
		MetaStructureProperty metaProperty =
			meta.getGlobalNamespace().getMetaProperty( predicate, false );
		if ( metaProperty == null )
		{
			throw new RuntimeException( "Unsupported predicate '" +
				predicate + "'" );
		}
		return metaProperty.node();
	}
}
