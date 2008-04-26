package org.neo4j.rdf.store.representation.standard;

import java.util.Collection;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.util.NeoPropertyArraySet;
import org.neo4j.util.index.Index;

public class PureQuadExecutor extends MetaEnabledUriBasedExecutor
{
    public PureQuadExecutor( NeoService neo, Index index, MetaStructure meta )
    {
        super( neo, index, meta );
    }

    @Override
    protected NodeAndRelationship createBlankNodeIfDoesntExist(
        AbstractNode startingAbstractNode, AbstractNode endingAbstractNode,
        AbstractRelationship abstractRelationship,
        Map<AbstractNode, Node> nodeMapping )
    {
        Node node = neo().createNode();
        debug( "\t+Node (literal) " + node );
        applyOnNode( endingAbstractNode, node );
        Node startNode = nodeMapping.get( startingAbstractNode );
        Relationship relationship = startNode.createRelationshipTo( node,
            new ARelationshipType(
                abstractRelationship.getRelationshipTypeName() ) );
        debug( "\t+Relationship " + startNode + " ---["
            + abstractRelationship.getRelationshipTypeName() +
            "]--> " + node );
        return new NodeAndRelationship( null, node, relationship );
    }

    @Override
    protected void doActualDeletion( Map<AbstractNode, Node> nodeMapping,
        Map<AbstractRelationship, Relationship> relationshipMapping )
    {
        for ( Map.Entry<AbstractRelationship, Relationship> entry :
            relationshipMapping.entrySet() )
        {
            AbstractRelationship abstractRelationship = entry.getKey();
            Relationship relationship = entry.getValue();
            removeFromRelationship( abstractRelationship, relationship,
                nodeMapping );
        }

        for ( Map.Entry<AbstractNode, Node> entry :
            nodeMapping.entrySet() )
        {
            if ( !isLiteralNode( entry.getKey() ) )
            {
                applyOnNode( entry.getKey(), entry.getValue() );
                if ( nodeIsEmpty( entry.getKey(), entry.getValue(), true ) )
                {
                    debug( "\t-Node " + entry.getValue() );
                    removeNode( entry.getValue(),
                        entry.getKey().getUriOrNull() );
                }
            }
        }
    }

    private void removeFromRelationship(
        AbstractRelationship abstractRelationship, Relationship relationship,
        Map<AbstractNode, Node> nodeMapping )
    {
        boolean triedToRemoveSomeContext = false;
        boolean removedSomeContext = false;
        for ( Map.Entry<String, Collection<Object>> entry :
            abstractRelationship.properties().entrySet() )
        {
            String key = entry.getKey();
            if ( !isContextKey( abstractRelationship, key ) )
            {
                throw new UnsupportedOperationException( key );
            }
            triedToRemoveSomeContext = true;
            Collection<Object> neoValues = new NeoPropertyArraySet<Object>(
                neo(), relationship, key );
            if ( removeAll( relationship, key, neoValues,
                entry.getValue(), "Property" ) )
            {
                removedSomeContext = true;
            }
        }

        if ( triedToRemoveSomeContext && !removedSomeContext )
        {
            return;
        }

        AbstractNode endNode = abstractRelationship.getEndNode();
        if ( !triedToRemoveSomeContext || contextRelationshipIsEmpty(
            abstractRelationship, relationship ) )
        {
            debug( "\t-Relationship " + relationship );
            relationship.delete();
            if ( isLiteralNode( endNode ) )
            {
                debug( "\t-Node " + nodeMapping.get( endNode ) );
                nodeMapping.get( endNode ).delete();
            }
        }
    }

    private boolean contextRelationshipIsEmpty(
        AbstractRelationship abstractRelationship, Relationship relationship )
    {
        return !relationship.hasProperty(
            StandardAbstractRepresentationStrategy.CONTEXT_PROPERTY_POSTFIX );
    }
}
