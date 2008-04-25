package org.neo4j.rdf.store.representation.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.validation.Validatable;

public class DenseValidatable extends AbstractValidatable
{
    public DenseValidatable( NeoService neo, Node node, MetaStructure meta )
    {
        super( neo, node, meta );
    }

    public Collection<? extends Validatable> complexProperties( String key )
    {
        Collection<Validatable> list = new ArrayList<Validatable>();
        for ( Relationship relationship : node().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( isPropertyRelationship( relationship ) )
            {
                list.add( new DenseValidatable( neoUtil().neo(),
                    relationship.getOtherNode( node() ), meta() ) );
            }
        }
        return list;
    }
    
    @Override
    protected void addSimplePropertyKeys( Set<String> set )
    {
        for ( String key : node().getPropertyKeys() )
        {
            if ( isPropertyKey( key ) )
            {
                set.add( key );
            }
        }
    }
    

    public Object[] getProperties( String key )
    {
        return neoUtil().getPropertyValues( node(), key ).toArray();
    }

    public boolean hasProperty( String key )
    {
        return node().hasProperty( key );
    }
}
