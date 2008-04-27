package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;

/**
 *
 * Each statement:
 * Subject to a middle node, to an object node (or literal node)
 * The middle node has relationships to which contexts that statement is in.
 *
 *    (S) ---[P]--> ( ) ---[P]--> (O)
 *                 /  \
 *                /    \
 *              [C]     [C]
 *              /        \
 *             v          v
 *           (C1)         (C2)
 *
 */
public class VerboseQuadStrategy
    extends StandardAbstractRepresentationStrategy
{
    public static final String EXECUTOR_INFO_NODE_TYPE = "nodetype";
    public static final String TYPE_SUBJECT = "subject";
    public static final String TYPE_MIDDLE = "middle";
    public static final String TYPE_LITERAL = "literal";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_CONTEXT = "context";

    public static final String KEY_OBJECT_URI_ON_MIDDLE_NODE = "object_uri";

    public VerboseQuadStrategy( RepresentationExecutor executor,
        MetaStructure meta )
    {
        super( executor, meta );
    }

    @Override
    public AbstractRepresentation getAbstractRepresentation(
        Statement statement )
    {
        AbstractRepresentation representation =
            super.getAbstractRepresentation( statement );
        if ( representation != null )
        {
            return representation;
        }

        if ( isObjectType( statement.getObject() ) )
        {
            representation = getObjectTypeRepresentation( statement );
        }
        else
        {
            representation = getLiteralRepresentation( statement );
        }
        return representation;
    }

    protected AbstractRepresentation getLiteralRepresentation(
        Statement statement )
    {
        AbstractRepresentation representation = newRepresentation();
        AbstractNode subjectNode = getSubjectNode( statement );
        representation.addNode( subjectNode );
        subjectNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_SUBJECT );
        AbstractNode middleNode = new AbstractNode( null );
        middleNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_MIDDLE );
        representation.addNode( middleNode );
        AbstractNode literalNode = new AbstractNode( null );
        literalNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_LITERAL );
        literalNode.addProperty( asUri( statement.getPredicate() ),
            ( ( Literal ) statement.getObject() ).getValue() );
        representation.addNode( literalNode );

        connectThreeNodes( representation, subjectNode, middleNode,
            literalNode, statement );
        addNodeToContexts( representation, middleNode, statement );
        return representation;
    }

    protected void connectThreeNodes(
        AbstractRepresentation representation, AbstractNode subjectNode,
        AbstractNode middleNode, AbstractNode otherNode, Statement statement )
    {
        String predicate = asUri( statement.getPredicate() );
        AbstractRelationship subjectToMiddle = new AbstractRelationship(
            subjectNode, predicate, middleNode );
        representation.addRelationship( subjectToMiddle );
        AbstractRelationship middleToOther = new AbstractRelationship(
            middleNode, predicate, otherNode );
        representation.addRelationship( middleToOther );
    }

    protected void addNodeToContexts(
        AbstractRepresentation representation, AbstractNode middleNode,
        Statement statement )
    {
        if ( !statement.getContext().isWildcard() )
        {
            AbstractNode contextNode = getContextNode(
                ( Context ) statement.getContext() );
            AbstractRelationship middleToContext = new AbstractRelationship(
                middleNode, RelTypes.IN_CONTEXT.name(), contextNode );
            representation.addRelationship( middleToContext );
            contextNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE,
                TYPE_CONTEXT );
            representation.addNode( contextNode );
        }
    }

    protected AbstractRepresentation getObjectTypeRepresentation(
        Statement statement )
    {
        AbstractRepresentation representation = newRepresentation();
        AbstractNode subjectNode = getSubjectNode( statement );
        representation.addNode( subjectNode );
        subjectNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_SUBJECT );
        AbstractNode middleNode = new AbstractNode( null );
        middleNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_MIDDLE );
        representation.addNode( middleNode );
        AbstractNode objectNode = getObjectNode( statement );
        objectNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_OBJECT );
        representation.addNode( objectNode );

        connectThreeNodes( representation, subjectNode, middleNode, objectNode,
            statement );
        addNodeToContexts( representation, middleNode, statement );
        return representation;
    }

    public static enum RelTypes implements RelationshipType
    {
        IN_CONTEXT,
    }
}
