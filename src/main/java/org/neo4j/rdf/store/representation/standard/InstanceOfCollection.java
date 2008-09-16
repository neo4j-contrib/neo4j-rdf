package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureRelTypes;
import org.neo4j.util.NeoRelationshipSet;

public class InstanceOfCollection extends NeoRelationshipSet<MetaStructureClass>
{
    private MetaStructure meta;
    
    public InstanceOfCollection( NeoService neo, MetaStructure meta, Node node )
    {
        super( neo, node, MetaStructureRelTypes.META_IS_INSTANCE_OF,
            Direction.OUTGOING );
        this.meta = meta;
    }

    @Override
    protected Node getNodeFromItem( Object item )
    {
        return ( ( MetaStructureClass ) item ).node();
    }

    @Override
    protected MetaStructureClass newObject( Node node,
        Relationship relationship )
    {
        return new MetaStructureClass( meta, node );
    }
}
