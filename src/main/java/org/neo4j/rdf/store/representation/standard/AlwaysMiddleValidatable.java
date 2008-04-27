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

public class AlwaysMiddleValidatable extends AbstractValidatable
{
    public AlwaysMiddleValidatable( NeoService neo, Node node,
        MetaStructure meta )
    {
        super( neo, node, meta );
    }

    @Override
    protected void addSimplePropertyKeys( Set<String> set )
    {
        addPropertyKeys( set, false );
    }

    @Override
    protected void addComplexPropertyKeys( Set<String> set )
    {
        addPropertyKeys( set, true );
    }

    private void addPropertyKeys( Set<String> set, boolean complex )
    {
        for ( Relationship rel : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( !isPredicateRelationship( rel ) )
            {
                continue;
            }
            Node middleNode = rel.getEndNode();
//            if ( middleNode.getSingleRelationship(
//                rel.getType(), Direction.OUTGOING ) == null )
//            {
//                System.out.println( "NULL for " + middleNode + ":" +
//                    rel.getType().name() );
//                for ( Relationship r : middleNode.getRelationships() )
//                {
//                    System.out.println( r.getStartNode() + " --[" +
//                        r.getType().name() + "]--> " + r.getEndNode() );
//                }
//            }
            Node otherNode = middleNode.getSingleRelationship(
                rel.getType(), Direction.OUTGOING ).getEndNode();
            if ( otherNode.hasProperty(
                AbstractUriBasedExecutor.URI_PROPERTY_KEY ) == complex )
            {
                set.add( rel.getType().name() );
            }
        }
    }

    public Collection<? extends Validatable> complexProperties( String key )
    {
        ArrayList<Validatable> list = new ArrayList<Validatable>();
        for ( Relationship rel : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( !isPredicateRelationship( rel ) )
            {
                continue;
            }
            Node middleNode = rel.getEndNode();
            Node otherNode = middleNode.getSingleRelationship(
                rel.getType(), Direction.OUTGOING ).getEndNode();
            if ( otherNode.hasProperty(
                AbstractUriBasedExecutor.URI_PROPERTY_KEY ) )
            {
                list.add( new AlwaysMiddleValidatable( neoUtil().neo(),
                    otherNode, meta() ) );
            }
        }
        return list;
    }

    public Object[] getProperties( String key )
    {
        ArrayList<Object> list = new ArrayList<Object>();
        for ( Relationship rel : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( !isPredicateRelationship( rel ) )
            {
                continue;
            }
            Node middleNode = rel.getEndNode();
            Node otherNode = middleNode.getSingleRelationship(
                rel.getType(), Direction.OUTGOING ).getEndNode();
            if ( !otherNode.hasProperty(
                AbstractUriBasedExecutor.URI_PROPERTY_KEY ) )
            {
                list.add( otherNode.getProperty( rel.getType().name() ) );
            }
        }
        return list.toArray();
    }

    public Node[] getPropertiesAsMiddleNodes( String key )
    {
        ArrayList<Node> list = new ArrayList<Node>();
        for ( Relationship rel : getUnderlyingNode().getRelationships(
            Direction.OUTGOING ) )
        {
            if ( !isPredicateRelationship( rel ) )
            {
                continue;
            }
            if ( !rel.getType().name().equals( key ) )
            {
                continue;
            }
            Node middleNode = rel.getEndNode();
            Node otherNode = middleNode.getSingleRelationship(
                rel.getType(), Direction.OUTGOING ).getEndNode();
//            if ( otherNode.hasProperty(
//                AbstractUriBasedExecutor.URI_PROPERTY_KEY ) == complex )
//            {
                list.add( middleNode );
//            }
        }
        return list.toArray( new Node[ list.size() ] );
    }

    public boolean hasProperty( String key )
    {
        return getProperties( key ).length > 0;
    }
}
