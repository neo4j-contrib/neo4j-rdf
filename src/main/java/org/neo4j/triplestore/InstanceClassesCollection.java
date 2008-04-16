package org.neo4j.triplestore;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureRelTypes;
import org.neo4j.util.NeoRelationshipSet;

/**
 * The classes an instance is "instanceof".
 */
public class InstanceClassesCollection
	extends NeoRelationshipSet<MetaStructureClass>
{
	private MetaStructure meta;
	
	/**
	 * @param meta the {@link MetaStructure}.
	 * @param instanceNode the instance node, f.ex. a subject or an object.
	 */
	public InstanceClassesCollection( MetaStructure meta, Node instanceNode )
	{
		super( instanceNode, MetaStructureRelTypes.META_IS_INSTANCE_OF,
			Direction.OUTGOING );
		this.meta = meta;
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		return ( ( MetaStructureClass ) item ).node();
	}
	
	@Override
	protected MetaStructureClass newObject( Node node, Relationship rel )
	{
		return new MetaStructureClass( meta, node );
	}
}
