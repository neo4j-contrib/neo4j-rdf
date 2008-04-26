package org.neo4j.rdf.store.representation.standard;

import java.util.ArrayList;
import java.util.Set;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.neometa.structure.MetaStructure;

public class PureQuadValidatable extends DenseValidatable
{
    public PureQuadValidatable( NeoService neo, Node node, MetaStructure meta )
    {
        super( neo, node, meta );
    }

    @Override
    protected void addSimplePropertyKeys( Set<String> set )
    {
        for ( Relationship relationship : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( isSimplePropertyRelationship( relationship ) )
            {
                set.add( relationship.getType().name() );
            }
        }
    }

    private boolean isSimplePropertyRelationship( Relationship relationship )
    {
        return isPredicateRelationship( relationship ) &&
            !relationship.getEndNode().hasProperty(
                UriBasedExecutor.URI_PROPERTY_KEY );
    }

    @Override
    public Object[] getProperties( String key )
    {
        ArrayList<Object> values = new ArrayList<Object>();
        for ( Relationship relationship : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( isSimplePropertyRelationship( relationship ) )
            {
                values.add( relationship.getEndNode().getProperty(
                    UriBasedExecutor.LITERAL_VALUE_KEY ) );
            }
        }
        return values.toArray();
    }

    @Override
    public boolean hasProperty( String key )
    {
        Object[] values = getProperties( key );
        return values != null && values.length > 0;
    }

    @Override
    protected boolean isPropertyRelationship( Relationship relationship )
    {
        return isPredicateRelationship( relationship ) &&
            relationship.getEndNode().hasProperty(
                UriBasedExecutor.URI_PROPERTY_KEY );
    }
}
