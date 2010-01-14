package org.neo4j.rdf.store.representation.standard;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelRelTypes;
import org.neo4j.util.NeoRelationshipSet;

public class InstanceOfCollection extends NeoRelationshipSet<MetaModelClass>
{
    private MetaModel meta;
    
    public InstanceOfCollection( GraphDatabaseService neo, MetaModel meta, Node node )
    {
        super( neo, node, MetaModelRelTypes.META_IS_INSTANCE_OF,
            Direction.OUTGOING );
        this.meta = meta;
    }

    @Override
    protected Node getNodeFromItem( Object item )
    {
        return ( ( MetaModelClass ) item ).node();
    }

    @Override
    protected MetaModelClass newObject( Node node,
        Relationship relationship )
    {
        return new MetaModelClass( meta, node );
    }
}
