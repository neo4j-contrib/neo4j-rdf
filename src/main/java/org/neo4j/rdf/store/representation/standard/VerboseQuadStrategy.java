package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
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
    public static final String EXECUTOR_INFO_PREDICATE = "predicate";
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
    public VerboseQuadExecutor getExecutor()
    {
        return ( VerboseQuadExecutor ) super.getExecutor();
    }

    @Override
    public AbstractRepresentation getAbstractRepresentation(
        Statement statement, AbstractRepresentation representation )
    {
        if ( super.getAbstractRepresentation( statement, representation ) !=
            null )
        {
            return representation;
        }
        
        if ( isObjectType( statement.getObject() ) )
        {
            getObjectTypeRepresentation( statement, representation );
        }
        else
        {
            getLiteralRepresentation( statement, representation );
        }
        return representation;
    }
    
    private String formMiddleNodeKey( Statement statement )
    {
        return "M" + formTripleNodeKey( statement );
    }

    protected AbstractRepresentation getLiteralRepresentation(
        Statement statement, AbstractRepresentation representation )
    {
        AbstractNode subjectNode = getOrCreateNode( representation,
            statement.getSubject() );
        subjectNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_SUBJECT );
        AbstractNode middleNode = getOrCreateNode( representation, null,
            formMiddleNodeKey( statement ) );
        middleNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_MIDDLE );
        AbstractNode literalNode = getOrCreateNode( representation, null,
            formTripleNodeKey( statement ) );
        literalNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_LITERAL );
        Literal literal = ( Literal ) statement.getObject();
        String predicate = asUri( statement.getPredicate() );
        literalNode.addProperty( predicate, literal.getValue() );
        literalNode.addExecutorInfo( EXECUTOR_INFO_PREDICATE, predicate );
        if ( literal.getDatatype() != null )
        {
            literalNode.addProperty( VerboseQuadExecutor.LITERAL_DATATYPE_KEY,
                literal.getDatatype().getUriAsString() );
        }
        if ( literal.getLanguage() != null )
        {
            literalNode.addProperty( VerboseQuadExecutor.LITERAL_LANGUAGE_KEY,
                literal.getLanguage() );
        }

        connectThreeNodes( representation, subjectNode, middleNode,
            literalNode, statement );
        connectMiddleNodeWithContext( representation, middleNode,
            statement );
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

    protected void connectMiddleNodeWithContext(
        AbstractRepresentation representation, AbstractNode middleNode,
        Statement statement )
    {
        if ( !statement.getContext().isWildcard() )
        {
            AbstractNode contextNode = getOrCreateNode( representation,
                statement.getContext() );
            AbstractRelationship middleToContext = new AbstractRelationship(
                middleNode, RelTypes.IN_CONTEXT.name(), contextNode );
            middleToContext.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE,
                TYPE_CONTEXT );
            representation.addRelationship( middleToContext );
            contextNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE,
                TYPE_CONTEXT );
        }
    }

    protected AbstractRepresentation getObjectTypeRepresentation(
        Statement statement, AbstractRepresentation representation )
    {
        AbstractNode subjectNode = getOrCreateNode( representation,
            statement.getSubject() );
        subjectNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_SUBJECT );
        AbstractNode middleNode = getOrCreateNode( representation, null,
            formMiddleNodeKey( statement ) );
        middleNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_MIDDLE );
        AbstractNode objectNode = getOrCreateNode( representation,
            statement.getObject() );
        objectNode.addExecutorInfo( EXECUTOR_INFO_NODE_TYPE, TYPE_OBJECT );

        connectThreeNodes( representation, subjectNode, middleNode, objectNode,
            statement );
        connectMiddleNodeWithContext( representation, middleNode,
            statement );
        return representation;
    }

    public static enum RelTypes implements RelationshipType
    {
        IN_CONTEXT,
    }
}
