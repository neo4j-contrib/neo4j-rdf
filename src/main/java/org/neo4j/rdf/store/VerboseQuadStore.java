package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.RelationshipTypeImpl;
import org.neo4j.rdf.store.representation.standard.VerboseQuadExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseQuadStrategy;
import org.neo4j.rdf.store.representation.standard.VerboseQuadValidatable;
import org.neo4j.rdf.validation.Validatable;
import org.neo4j.util.index.IndexService;

public class VerboseQuadStore extends RdfStoreImpl
{
    private final MetaStructure meta;

    public VerboseQuadStore( NeoService neo, IndexService indexer )
    {
        this( neo, indexer, null );
    }

    public VerboseQuadStore( NeoService neo, IndexService indexer,
        MetaStructure meta )
    {
        super( neo, new VerboseQuadStrategy(
            new VerboseQuadExecutor( neo, indexer, meta ), meta ) );
        this.meta = meta;
    }

    protected MetaStructure meta()
    {
        return this.meta;
    }

    @Override
    protected VerboseQuadStrategy getRepresentationStrategy()
    {
        return ( VerboseQuadStrategy ) super.getRepresentationStrategy();
    }

    @Override
    public Iterable<CompleteStatement> getStatements(
        WildcardStatement statement,
        boolean includeInferredStatements )
    {
        Transaction tx = neo().beginTx();
        try
        {
            if ( includeInferredStatements )
            {
                throw new UnsupportedOperationException( "We currently not " +
                    "support getStatements() with reasoning enabled" );
            }

            Iterable<CompleteStatement> result = null;
            if ( wildcardPattern( statement, false, false, true ) )
            {
                result = handleSubjectPredicateWildcard( statement );
            }
            else if ( wildcardPattern( statement, false, true, true ) )
            {
                result = handleSubjectWildcardWildcard( statement );
            }
            else if ( wildcardPattern( statement, false, true, false ) )
            {
                result = handleSubjectWilcardObject( statement );
            }
            else if ( wildcardPattern( statement, true, true, false ) )
            {
                result = handleWildcardWildcardObject( statement );
            }
            else if ( wildcardPattern( statement, true, false, false ) )
            {
                result = handleWildcardPredicateObject( statement );
            }
            else if ( wildcardPattern( statement, false, false, false ) )
            {
                result = handleSubjectPredicateObject( statement );
            }
            else if ( wildcardPattern( statement, true, true, true ) )
            {
                result = handleWildcardWildcardWildcard( statement );
            }
            else
            {
                result = super.getStatements( statement,
                    includeInferredStatements );
            }

            tx.success();
            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    private Node lookupNode( Value uri )
    {
        return getRepresentationStrategy().getExecutor().
            lookupNode( new AbstractNode( uri ) );
    }

    private Iterable<CompleteStatement> handleWildcardWildcardWildcard(
        Statement statement )
    {
        if ( statement.getContext().isWildcard() )
        {
            throw new RuntimeException( "We can't handle ?S ?P ?O ?G" );
        }

        Context context = ( Context ) statement.getContext();
        Node contextNode = lookupNode( context );
        if ( contextNode == null )
        {
            return new ArrayList<CompleteStatement>();
        }

        List<CompleteStatement> statementList =
            new LinkedList<CompleteStatement>();
        for ( Relationship contextRelationship : contextNode.getRelationships(
            VerboseQuadStrategy.RelTypes.IN_CONTEXT, Direction.INCOMING ) )
        {
            Node middleNode = contextRelationship.getStartNode();
            Relationship subjectRelationship = findSubjectRelationship(
                middleNode );
            if ( subjectRelationship == null )
            {
                throw new RuntimeException( "Error, no subject for " +
                    middleNode );
            }
            Node subjectNode = subjectRelationship.getOtherNode( middleNode );
            Node objectNode = getObjectNode(
                middleNode, subjectRelationship.getType().name() );
            Uri subject = newValidatable( subjectNode ).getUri();
            Uri predicate = new Uri( subjectRelationship.getType().name() );
            Value object = getValueForObjectNode( predicate.getUriAsString(),
                objectNode );
            if ( object instanceof Literal )
            {
                statementList.add( new CompleteStatement(
                    subject, predicate, ( Literal ) object, context ) );
            }
            else
            {
                statementList.add( new CompleteStatement(
                    subject, predicate, ( Uri ) object, context ) );
            }
        }
        return statementList;
    }

    private Relationship findSubjectRelationship( Node middleNode )
    {
        for ( Relationship relationship : middleNode.getRelationships(
            Direction.INCOMING ) )
        {
            if ( !relationship.getType().name().equals( VerboseQuadStrategy.
                RelTypes.IN_CONTEXT.name() ) )
            {
                return relationship;
            }
        }
        return null;
    }

    private Iterable<CompleteStatement> handleSubjectWilcardObject(
        Statement statement )
    {
        ArrayList<CompleteStatement> statementList =
            new ArrayList<CompleteStatement>();
        Uri subject = ( Uri ) statement.getSubject();
        Node subjectNode = lookupNode( subject );
        if ( subjectNode == null )
        {
            return statementList;
        }

        VerboseQuadValidatable validatable = newValidatable( subjectNode );
        if ( statement.getObject() instanceof Uri )
        {
            Uri object = ( Uri ) statement.getObject();
            Node objectNode = lookupNode( object );
            if ( objectNode == null )
            {
                return statementList;
            }

            for ( String key : validatable.getComplexPropertyKeys() )
            {
                for ( Validatable otherObject : validatable.complexProperties(
                    key ) )
                {
                    Node middleNode = otherObject.getUnderlyingNode().
                        getSingleRelationship( new RelationshipTypeImpl( key ),
                        Direction.INCOMING ).getStartNode();
                    if ( otherObject.getUnderlyingNode().equals( objectNode ) )
                    {
                        addIfInContext( statement, statementList,
                            middleNode, key );
                    }
                }
            }
        }
        else
        {
            Literal literal = ( Literal ) statement.getObject();
            Object value = literal.getValue();
            for ( String key : validatable.getSimplePropertyKeys() )
            {
                for ( Node middleNode : validatable.getPropertiesAsMiddleNodes(
                    key ) )
                {
                    if ( literalMatches( middleNode, key, value ) )
                    {
                        addIfInContext( statement, statementList, middleNode,
                            key );
                    }
                }
            }
        }
        return statementList;
    }

    private Iterable<CompleteStatement> handleSubjectPredicateObject(
        Statement statement )
    {
        ArrayList<CompleteStatement> statementList =
            new ArrayList<CompleteStatement>();
        Uri subject = ( Uri ) statement.getSubject();
        Uri predicate = ( Uri ) statement.getPredicate();
        RelationshipType predicateType = new RelationshipTypeImpl(
            predicate.getUriAsString() );
        Node subjectNode = lookupNode( subject );
        if ( subjectNode == null )
        {
            return statementList;
        }

        VerboseQuadValidatable validatable = newValidatable( subjectNode );
        if ( statement.getObject() instanceof Uri )
        {
            Node objectNode = lookupNode( statement.getObject() );
            Relationship objectToMiddle = objectNode.getSingleRelationship(
                predicateType, Direction.INCOMING );
            if ( objectToMiddle == null )
            {
                return statementList;
            }

            Node middleNode = objectNode.getSingleRelationship(
                predicateType, Direction.INCOMING ).getStartNode();
            if ( objectNode == null )
            {
                return new ArrayList<CompleteStatement>();
            }
            for ( Validatable complexProperty : validatable.complexProperties(
                predicate.getUriAsString() ) )
            {
                if ( complexProperty.getUnderlyingNode().equals( objectNode ) )
                {
                    addIfInContext( statement, statementList, middleNode,
                        predicate.getUriAsString() );
                }
            }
        }
        else
        {
            Object value = ( ( Literal ) statement.getObject() ).getValue();
            for ( Node middleNode : validatable.getPropertiesAsMiddleNodes(
                predicate.getUriAsString() ) )
            {
                if ( literalMatches( middleNode, predicate.getUriAsString(),
                    value ) )
                {
                    addIfInContext( statement, statementList, middleNode,
                        predicate.getUriAsString() );
                }
            }
        }
        return statementList;
    }

    private boolean literalMatches( Node middleNode, String key, Object value )
    {
        Node literalNode = middleNode.getSingleRelationship(
            new RelationshipTypeImpl( key ), Direction.OUTGOING ).getEndNode();
        Object literalValue = literalNode.getProperty( key );
        return value.equals( literalValue );
    }

    private Node getObjectNode( Node middleNode, String key )
    {
        return middleNode.getSingleRelationship(
            new RelationshipTypeImpl( key ), Direction.OUTGOING ).getEndNode();
    }

    private Iterable<CompleteStatement> handleWildcardPredicateObject(
        WildcardStatement statement )
    {
        return handleWildcardWildcardObject( statement );
    }

    private VerboseQuadValidatable newValidatable( Node subjectNode )
    {
        return new VerboseQuadValidatable( neo(), subjectNode, meta() );
    }

    private void buildStatementsOfObjectHits( Statement statement,
        List<CompleteStatement> statementList, Literal literal )
    {
        VerboseQuadExecutor executor = ( VerboseQuadExecutor )
            getRepresentationStrategy().getExecutor();
        Object value = literal.getValue();
        String predicate = statement.getPredicate() instanceof Wildcard ?
            null : ( ( Uri ) statement.getPredicate() ).getUriAsString();
        for ( Node literalNode : executor.findLiteralNodes( value ) )
        {
            for ( Relationship rel : literalNode.getRelationships(
                Direction.INCOMING ) )
            {
                Node middleNode = rel.getStartNode();
                Uri thePredicate = new Uri( rel.getType().name() );
                if ( predicate != null &&
                    !thePredicate.getUriAsString().equals( predicate ) )
                {
                    continue;
                }

                addIfInContext( statement, statementList, middleNode,
                    thePredicate.getUriAsString() );
            }
        }
    }

    private void buildStatementsOfObjectHits( Statement statement,
        List<CompleteStatement> statementList, Uri object )
    {
        Uri objectUri = ( Uri ) statement.getObject();
        Node objectNode = getRepresentationStrategy().getExecutor().
            lookupNode( new AbstractNode( objectUri ) );
        if ( objectNode == null )
        {
            return;
        }

        String predicate = statement.getPredicate() instanceof Wildcard ?
            null : ( ( Uri ) statement.getPredicate() ).getUriAsString();
        for ( Relationship rel : objectNode.getRelationships(
            Direction.INCOMING ) )
        {
            Node middleNode = rel.getStartNode();
            Uri thePredicate = new Uri( rel.getType().name() );
            if ( predicate != null &&
                !thePredicate.getUriAsString().equals( predicate ) )
            {
                continue;
            }

            addIfInContext( statement, statementList, middleNode,
                thePredicate.getUriAsString() );
        }
    }

    private RelationshipType relType( final String name )
    {
        return new RelationshipType()
        {
            public String name()
            {
                return name;
            }
        };
    }

    private static Context getContextForUri( String contextUri )
    {
        return isNull( contextUri ) ? null : new Context( contextUri );
    }

    private static boolean isNull( String uri )
    {
        return uri == null || uri.equals( "null" );
    }

    private Set<Context> getExistingContexts( Node middleNode )
    {
        Set<Context> set = new HashSet<Context>();
        for ( Relationship relationship : middleNode.getRelationships(
            VerboseQuadStrategy.RelTypes.IN_CONTEXT,
            Direction.OUTGOING ) )
        {
            Node contextNode = relationship.getEndNode();
            String uri = ( String ) contextNode.getProperty(
                AbstractUriBasedExecutor.URI_PROPERTY_KEY );
            set.add( getContextForUri( uri ) );
        }
        return set;
    }

    private void addIfInContext( Statement statement,
        List<CompleteStatement> statementList, Node middleNode,
        String predicate )
    {
        Relationship rel = middleNode.getSingleRelationship(
            relType( predicate ), Direction.INCOMING );
        Node subjectNode = rel.getStartNode();
        Validatable validatable = newValidatable( subjectNode );
        Uri subject = validatable.getUri();
        Set<Context> existingContexts = getExistingContexts( middleNode );
        Set<Context> contextsToAdd = new HashSet<Context>();
        if ( statement.getContext() instanceof Wildcard )
        {
            contextsToAdd = existingContexts;
        }
        else
        {
            if ( existingContexts.contains( statement.getContext() ) )
            {
                contextsToAdd.add( ( Context ) statement.getContext() );
            }
        }

        for ( Context context : contextsToAdd )
        {
            Node objectNode = middleNode.getSingleRelationship(
                relType( predicate ), Direction.OUTGOING ).getEndNode();
            Value object = getValueForObjectNode( predicate, objectNode );
            if ( object instanceof Resource )
            {
                statementList.add( new CompleteStatement( subject,
                    new Uri( predicate ), ( Resource ) object, context ) );
            }
            else
            {
                statementList.add( new CompleteStatement( subject,
                    new Uri( predicate ), ( Literal ) object, context ) );
            }
        }
    }

    private Value getValueForObjectNode( String predicate, Node objectNode )
    {
        String uri = ( String ) objectNode.getProperty(
            AbstractUriBasedExecutor.URI_PROPERTY_KEY, null );
        if ( uri != null )
        {
            return new Uri( uri );
        }
        else
        {
            Object value = objectNode.getProperty( predicate );
            String datatype = ( String ) objectNode.getProperty(
                VerboseQuadExecutor.LITERAL_DATATYPE_KEY, null );
            String language = ( String ) objectNode.getProperty(
                VerboseQuadExecutor.LITERAL_LANGUAGE_KEY, null );
            return new Literal( value, datatype == null ? null :
                new Uri( datatype ), language );
        }
    }

    private Iterable<CompleteStatement> handleWildcardWildcardObject(
        WildcardStatement statement )
    {
        List<CompleteStatement> statementList =
            new ArrayList<CompleteStatement>();
        if ( statement.getObject() instanceof Literal )
        {
            buildStatementsOfObjectHits( statement, statementList,
                ( Literal ) statement.getObject() );
        }
        else
        {
            buildStatementsOfObjectHits( statement, statementList,
                ( Uri ) statement.getObject() );
        }
        return statementList;
    }

    private Iterable<CompleteStatement> handleSubjectPredicateWildcard(
        WildcardStatement statement )
    {
        Uri subject = ( Uri ) statement.getSubject();
        Uri predicate = ( Uri ) statement.getPredicate();

        AbstractNode abstractSubjectNode = new AbstractNode( subject );
        Node subjectNode = getRepresentationStrategy().getExecutor().
            lookupNode( abstractSubjectNode );
        if ( subjectNode == null )
        {
            return new ArrayList<CompleteStatement>();
        }

        VerboseQuadValidatable validatableInstance =
            newValidatable( subjectNode );
        List<CompleteStatement> statementList =
            new LinkedList<CompleteStatement>();
        addObjects( statement, statementList, subject, predicate, subjectNode,
            validatableInstance );
        return statementList;
    }

    private void addObjects( Statement statement,
        List<CompleteStatement> statementList,
        Uri subject, Uri predicate, Node subjectNode,
        VerboseQuadValidatable validatableInstance )
    {
        Node[] middleNodes = validatableInstance.
            getPropertiesAsMiddleNodes( predicate.getUriAsString() );
        for ( Node middleNode : middleNodes )
        {
            addIfInContext( statement, statementList, middleNode,
                predicate.getUriAsString() );
        }
    }

    private void addObjects( Statement statement,
        List<CompleteStatement> statementList,
        Uri subject, Node subjectNode, VerboseQuadValidatable instance )
    {
        for ( String predicate : instance.getAllPropertyKeys() )
        {
            addObjects( statement, statementList, subject, new Uri( predicate ),
                subjectNode, instance );
        }
//        for ( MetaStructureClass cls : validatableInstance.getClasses() )
//        {
//            statementList.add( new CompleteStatement( subject, new Uri(
//                AbstractUriBasedExecutor.RDF_TYPE_URI ), new Uri(
//                    cls.getName() ) ) );
//        }
    }

    private Iterable<CompleteStatement> handleSubjectWildcardWildcard(
        WildcardStatement statement )
    {
        Uri subject = ( Uri ) statement.getSubject();
        AbstractNode abstractSubjectNode = new AbstractNode( subject );
        Node subjectNode = getRepresentationStrategy().getExecutor().
            lookupNode( abstractSubjectNode );
        if ( subjectNode == null )
        {
            return new ArrayList<CompleteStatement>();
        }

        VerboseQuadValidatable validatableInstance =
            newValidatable( subjectNode );
        List<CompleteStatement> statementList =
            new ArrayList<CompleteStatement>();
        addObjects( statement, statementList, subject,
            subjectNode, validatableInstance );
        return statementList;
    }
}
