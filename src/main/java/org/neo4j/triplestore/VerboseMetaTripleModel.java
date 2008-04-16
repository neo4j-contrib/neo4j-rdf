package org.neo4j.triplestore;

import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;

/**
 * Uses a {@link MetaStructure} to find the predicate node.
 */
public class VerboseMetaTripleModel extends VerboseTripleModel
{
	private MetaStructure meta;
	
	/**
	 * @param meta the {@link MetaStructure}.
	 */
	public VerboseMetaTripleModel( MetaStructure meta )
	{
		super( meta.neo() );
		this.meta = meta;
	}
	
	@Override
	protected Node getPredicateNode( String predicate )
	{
		return meta.getGlobalNamespace().getMetaProperty(
			predicate, true ).node();
	}
}
