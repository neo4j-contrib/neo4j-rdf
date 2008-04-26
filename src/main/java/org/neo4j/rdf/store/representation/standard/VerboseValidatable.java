package org.neo4j.rdf.store.representation.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.validation.Validatable;

public class VerboseValidatable extends AbstractValidatable
{
    public VerboseValidatable( NeoService neo, Node node,
        MetaStructure meta )
    {
        super( neo, node, meta );
    }
    
    @Override
    protected boolean isPropertyRelationship( Relationship relationship )
    {
        return isSimpleOrComplexRelationship( relationship, true );
    }
    
    private boolean isSimpleOrComplexRelationship( Relationship relationship,
        boolean complex )
    {
        Node connectorNode = relationship.getOtherNode( getUnderlyingNode() );
        return connectorNode.hasRelationship(
            VerboseRepresentationStrategy.RelTypes.CONNECTOR_HAS_PREDICATE ) &&
            connectorNode.hasRelationship(
                relationship.getType(), Direction.OUTGOING ) == complex;
    }
    
    @Override
    protected void addSimplePropertyKeys( Set<String> set )
    {
        for ( Relationship relationship : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( isSimpleOrComplexRelationship( relationship, false ) )
            {
                set.add( relationship.getType().name() );
            }
        }
    }

    public Collection<? extends Validatable> complexProperties( String key )
    {
        Collection<Validatable> list = new ArrayList<Validatable>();
        for ( Relationship relationship : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( !isSimpleOrComplexRelationship( relationship, true ) )
            {
                continue;
            }
            
            Node objectNode = relationship.getOtherNode( getUnderlyingNode() ).
                getSingleRelationship( relationship.getType(),
                    Direction.OUTGOING ).getEndNode();
            list.add( new VerboseValidatable( neoUtil().neo(), objectNode,
                meta() ) );
        }
        return list;
    }

    public Object[] getProperties( String key )
    {
        Node connectorNode = getSimplePropertyNode( key );
        return connectorNode == null ? new Object[] {} :
            neoUtil().getPropertyValues( connectorNode, key ).toArray();
    }
    
    private Node getSimplePropertyNode( String key )
    {
        RelationshipType type = new UriBasedExecutor.ARelationshipType( key );
        for ( Relationship relationship : getUnderlyingNode().getRelationships(
            type, Direction.OUTGOING ) )
        {
            // Should only have zero or one relationship.
            if ( isSimpleOrComplexRelationship( relationship, false ) )
            {
                return relationship.getOtherNode( getUnderlyingNode() );
            }
        }
        return null;
    }

    public boolean hasProperty( String key )
    {
        return getSimplePropertyNode( key ) != null;
    }
}
