package org.neo4j.rdf.store;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.neo4j.commons.Predicate;
import org.neo4j.commons.iterator.FilteringIterable;
import org.neo4j.commons.iterator.FilteringIterator;
import org.neo4j.commons.iterator.IterableWrapper;
import org.neo4j.commons.iterator.NestingIterable;
import org.neo4j.commons.iterator.NestingIterator;
import org.neo4j.commons.iterator.PrefetchingIterator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.IndexService;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.fulltext.QueryResult;
import org.neo4j.rdf.fulltext.RawQueryResult;
import org.neo4j.rdf.fulltext.VerificationHook;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.StatementMetadata;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseQuadExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseQuadStrategy;
import org.neo4j.util.RelationshipToNodeIterable;

/**
 * An {@link RdfStore} capable of storing quads, i.e. Subject, Predicate, Object, Context.
 */
public class VerboseQuadStore extends RdfStoreImpl
{
    private final MetaModel model;
    
    public VerboseQuadStore( GraphDatabaseService graphDb, IndexService indexer )
    {
        this( graphDb, indexer, null, null );
    }
    
    public VerboseQuadStore( GraphDatabaseService graphDb, IndexService indexer,
        MetaModel model, FulltextIndex fulltextIndex )
    {
        super( graphDb, new VerboseQuadStrategy( new VerboseQuadExecutor( graphDb,
            indexer, model, fulltextIndex ), model ) );
        this.model = model;
        debug( "I'm initialized!" );
    }
    
    /**
     * Provided if you'd like to customize the {@link VerboseQuadStrategy}
     * to fit your needs.
     * 
     * @param graphDb the {@link GraphDatabaseService} to use.
     * @param strategy the {@link VerboseQuadStrategy} to use.
     */
    protected VerboseQuadStore( GraphDatabaseService graphDb, VerboseQuadStrategy strategy )
    {
        super( graphDb, strategy );
        this.model = null;
    }
    
    protected MetaModel model()
    {
        return this.model;
    }
    
    @Override
    public VerboseQuadStrategy getRepresentationStrategy()
    {
        return ( VerboseQuadStrategy ) super.getRepresentationStrategy();
    }
    
    @Override
    public Iterable<CompleteStatement> getStatements(
        WildcardStatement statement,
        boolean includeInferredStatements )
    {
        //        debug( "getStatements( " + statement + " )" );
        Transaction tx = graphDb().beginTx();
        try
        {
            if ( includeInferredStatements )
            {
                throw new UnsupportedOperationException( "We currently don't " +
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
                result = handleSubjectWildcardObject( statement );
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
            else if ( wildcardPattern( statement, true, false, true ) )
            {
                result = handleWildcardPredicateWildcard( statement );
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
            
            if ( result == null )
            {
                result = new LinkedList<CompleteStatement>();
            }
            
            tx.success();
            return result;
        }
        finally
        {
            tx.finish();
        }
    }
    
    public void reindexFulltextIndex( Integer maxEntries )
    {
        Transaction tx = graphDb().beginTx();
        try
        {
            Iterable<Node> allMiddleNodes = getMiddleNodesFromAllContexts();
            Iterable<Object[]> allQuads = new MiddleNodeToQuadIterable(
                new WildcardStatement( new Wildcard( "s" ), new Wildcard( "p" ),
                    new Wildcard( "o" ), new Wildcard( "g" ) ),
                    allMiddleNodes );
            int totalCounter = 0;
            int counter = 0;
            FulltextIndex fulltextIndex = getFulltextIndex();
            fulltextIndex.clear();
            for ( Object[] quad : allQuads )
            {
                String predicate = ( String ) quad[ 1 ];
                Node objectNode = ( Node ) quad[ 2 ];
                Value objectValue =
                    getValueForObjectNode( predicate, objectNode );
                if ( objectValue instanceof Literal )
                {
                    fulltextIndex.index( objectNode, new Uri( predicate ),
                        ( ( Literal ) objectValue ).getValue() );
                    if ( ++counter % 10000 == 0 )
                    {
                        fulltextIndex.end( true );
                        tx.success();
                        tx.finish();
                        tx = graphDb().beginTx();
                        if ( maxEntries != null && counter > maxEntries )
                        {
                            break;
                        }
                    }
                }
                
                if ( ++totalCounter % 1000 == 0 )
                {
                    // TODO Notify somehow?
                }
            }
            fulltextIndex.end( true );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
    
    @Override
    public void reindexFulltextIndex()
    {
        reindexFulltextIndex( null );
    }
    
    @Override
    public Iterable<QueryResult> searchFulltext( String query )
    {
        return searchFulltextWithSnippets( query, 0 );
    }
    
    protected FulltextIndex getInitializedFulltextIndex()
    {
        FulltextIndex fulltextIndex = getFulltextIndex();
        if ( fulltextIndex == null )
        {
            throw new RuntimeException( "No fulltext index instantiated, " +
                "please supply a FulltextIndex instance at construction time " +
            "to get this feature" );
        }
        return fulltextIndex;
    }
    
    @Override
    public Iterable<QueryResult> searchFulltextWithSnippets( String query,
        int snippetCountLimit )
    {
        FulltextIndex fulltextIndex = getInitializedFulltextIndex();
        Iterable<RawQueryResult> rawResult = snippetCountLimit == 0 ?
            fulltextIndex.search( query ) :
            fulltextIndex.searchWithSnippets( query, snippetCountLimit );
        final RawQueryResult[] latestQueryResult = new RawQueryResult[ 1 ];
        Iterable<Node> middleNodes = new LiteralToMiddleNodeIterable(
            new IterableWrapper<Node, RawQueryResult>( rawResult )
            {
                @Override
                protected Node underlyingObjectToObject( RawQueryResult object )
                {
                    latestQueryResult[ 0 ] = object;
                    return object.getNode();
                }
            } );
        
        Statement fakeWildcardStatement = new WildcardStatement(
            new Wildcard( "S" ), new Wildcard( "P" ),
            new Wildcard( "O" ), new Wildcard( "C" ) );
        Iterable<CompleteStatement> statementIterator = statementIterator(
            fakeWildcardStatement, middleNodes );
        return new IterableWrapper<QueryResult, CompleteStatement>(
            statementIterator )
        {
            @Override
            protected QueryResult underlyingObjectToObject(
                CompleteStatement object )
            {
                return new QueryResult( object,
                    latestQueryResult[ 0 ].getScore(),
                    latestQueryResult[ 0 ].getSnippet() );
            }
        };
    }
    
    public boolean verifyFulltextIndex( String queryOrNullForAll )
    {
        Transaction tx = graphDb().beginTx();
        try
        {
            boolean result = getInitializedFulltextIndex().verify(
                new QuadVerificationHook(), queryOrNullForAll );
            tx.success();
            return result;
        }
        finally
        {
            tx.finish();
        }
    }
    
    @Override
    public int size( Context... contexts )
    {
        Transaction tx = graphDb().beginTx();
        try
        {
            Iterable<Node> contextNodes = null;
            if ( contexts.length == 0 )
            {
                Node contextRefNode = getRepresentationStrategy().getExecutor().
                    getContextsReferenceNode();
                contextNodes = new RelationshipToNodeIterable( contextRefNode,
                    contextRefNode.getRelationships(
                        VerboseQuadExecutor.RelTypes.IS_A_CONTEXT,
                        Direction.OUTGOING ) );
            }
            else
            {
                ArrayList<Node> nodes = new ArrayList<Node>();
                for ( Context context : contexts )
                {
                    Node node = getRepresentationStrategy().getExecutor().
                    lookupNode( new AbstractNode( context ) );
                    if ( node != null )
                    {
                        nodes.add( node );
                    }
                }
                contextNodes = nodes;
            }
            
            int size = 0;
            for ( Node node : contextNodes )
            {
                size += ( Integer ) node.getProperty(
                    VerboseQuadExecutor.STATEMENT_COUNT, 0 );
            }
            tx.success();
            return size;
        }
        finally
        {
            tx.finish();
        }
    }
    
    private void debug( String message )
    {
        //        System.out.println( "====> VerboseQuadStore: " + message );
    }
    
    private String getNodeUriOrNull( Node node )
    {
        return ( String ) node.getProperty(
            AbstractUriBasedExecutor.URI_PROPERTY_KEY, null );
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
            Object value = objectNode.getProperty(
                getRepresentationStrategy().getExecutor()
                    .getLiteralNodePropertyKey( predicate ) );
            String datatype = ( String ) objectNode.getProperty(
                VerboseQuadExecutor.LITERAL_DATATYPE_KEY, null );
            String language = ( String ) objectNode.getProperty(
                VerboseQuadExecutor.LITERAL_LANGUAGE_KEY, null );
            return new Literal( value, datatype == null ? null :
                new Uri( datatype ), language );
        }
    }
    
    private Iterable<Node> getMiddleNodesFromLiterals( Statement statement )
    {
        Literal literal = ( Literal ) statement.getObject();
        Iterable<Node> literalNodes = getRepresentationStrategy().
            getExecutor().findLiteralNodes( literal.getValue() );
        return new LiteralToMiddleNodeIterable( literalNodes );
    }
    
    private Iterable<Node> getMiddleNodesFromAllContexts()
    {
        Node contextsRefNode = getRepresentationStrategy().getExecutor().
            getContextsReferenceNode();
        Iterable<Node> contexts = new RelationshipToNodeIterable(
            contextsRefNode, contextsRefNode.getRelationships(
            VerboseQuadExecutor.RelTypes.IS_A_CONTEXT, Direction.OUTGOING ) );
        
        return new NestingIterable<Node, Node>( contexts )
        {
            private Predicate<Node> predicate = new Predicate<Node>()
            {
                private final Set<Long> set = new HashSet<Long>();
                
                public boolean accept( Node item )
                {
                    return set.add( item.getId() );
                }
            };
            
            @Override
            protected Iterator<Node> createNestedIterator( Node contextNode )
            {
                return new FilteringIterator<Node>( new RelationshipToNodeIterable( contextNode,
                        contextNode.getRelationships( VerboseQuadStrategy.RelTypes.
                                IN_CONTEXT, Direction.INCOMING ) ).iterator(), predicate );
            }
        };
        
        // Traversers are too slow, and it's so much manlier to do it manually
//        return getRepresentationStrategy().getExecutor().
//        getContextsReferenceNode().traverse( Order.DEPTH_FIRST,
//            StopEvaluator.END_OF_GRAPH,
//            new OneOfRelTypesReturnableEvaluator(
//                VerboseQuadStrategy.RelTypes.IN_CONTEXT ),
//                VerboseQuadExecutor.RelTypes.IS_A_CONTEXT, Direction.OUTGOING,
//                VerboseQuadStrategy.RelTypes.IN_CONTEXT, Direction.INCOMING );
    }
    
    private Iterable<CompleteStatement> handleSubjectPredicateWildcard(
        Statement statement )
    {
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }
        Iterable<Node> middleNodes = new RelationshipToNodeIterable(
            subjectNode, subjectNode.getRelationships( relType( statement ),
                Direction.OUTGOING ) );
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> handleSubjectWildcardWildcard(
        Statement statement )
    {
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }
        Iterable<Node> middleNodes = new RelationshipToNodeIterable(
            subjectNode, subjectNode.getRelationships( Direction.OUTGOING ) );
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> handleSubjectWildcardObject(
        final Statement statement )
    {
        // TODO Optimization: maybe check which has least rels (S or O)
        // and start there.
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }
        Iterable<Relationship> relationships =
            subjectNode.getRelationships( Direction.OUTGOING );
        relationships = new ObjectFilteredRelationships( statement,
            relationships );
        Iterable<Node> middleNodes = new RelationshipToNodeIterable(
            subjectNode, relationships );
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> handleSubjectPredicateObject(
        Statement statement )
    {
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }
        Iterable<Relationship> relationships = subjectNode.getRelationships(
            relType( statement ), Direction.OUTGOING );
        relationships = new ObjectFilteredRelationships( statement,
            relationships );
        Iterable<Node> middleNodes = new RelationshipToNodeIterable(
            subjectNode, relationships );
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> handleWildcardWildcardObject(
        Statement statement )
    {
        Iterable<Node> middleNodes = null;
        if ( statement.getObject() instanceof Literal )
        {
            middleNodes = getMiddleNodesFromLiterals( statement );
        }
        else
        {
            Node objectNode = lookupNode( statement.getObject() );
            if ( objectNode == null )
            {
                return null;
            }
            middleNodes = new RelationshipToNodeIterable(
                objectNode, objectNode.getRelationships( Direction.INCOMING ) );
        }
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> handleWildcardPredicateWildcard(
        Statement statement )
    {
        Iterable<Node> middleNodes = null;
        if ( statement.getContext().isWildcard() )
        {
            middleNodes = getMiddleNodesFromAllContexts();
        }
        else
        {
            Node contextNode = lookupNode( statement.getContext() );
            if ( contextNode == null )
            {
                return null;
            }
            middleNodes = new RelationshipToNodeIterable(
                contextNode, contextNode.getRelationships(
                    VerboseQuadStrategy.RelTypes.IN_CONTEXT,
                    Direction.INCOMING ) );
        }
        middleNodes = new PredicateFilteredNodes( statement, middleNodes );
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> handleWildcardPredicateObject(
        Statement statement )
    {
        Iterable<Node> middleNodes = null;
        if ( statement.getObject() instanceof Literal )
        {
            middleNodes = new PredicateFilteredNodes( statement,
                getMiddleNodesFromLiterals( statement ) );
        }
        else
        {
            Node objectNode = lookupNode( statement.getObject() );
            if ( objectNode == null )
            {
                return null;
            }
            middleNodes = new RelationshipToNodeIterable(
                objectNode, objectNode.getRelationships( relType( statement ),
                    Direction.INCOMING ) );
        }
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> handleWildcardWildcardWildcard(
        Statement statement )
    {
        Iterable<Node> middleNodes = null;
        if ( statement.getContext().isWildcard() )
        {
            middleNodes = getMiddleNodesFromAllContexts();
        }
        else
        {
            Node contextNode = lookupNode( statement.getContext() );
            if ( contextNode == null )
            {
                return null;
            }
            middleNodes = new RelationshipToNodeIterable(
                contextNode, contextNode.getRelationships(
                    VerboseQuadStrategy.RelTypes.IN_CONTEXT,
                    Direction.INCOMING ) );
        }
        return statementIterator( statement, middleNodes );
    }
    
    private Iterable<CompleteStatement> statementIterator(
        Statement statement, Iterable<Node> middleNodes )
    {
        return new QuadToStatementIterable( new MiddleNodeToQuadIterable(
            statement, middleNodes ) );
        
        // Enable this when we implement inferencing.
        //    	return new QuadToStatementIterable(
        //    		new QuadWithInferencingIterable(
        //    		new MiddleNodeToQuadIterable( statement, middleNodes ) ) );
    }
    
    private class QuadToStatementIterable
        extends IterableWrapper<CompleteStatement, Object[]>
    {
        QuadToStatementIterable( Iterable<Object[]> source )
        {
            super( source );
        }
        
        @Override
        protected CompleteStatement underlyingObjectToObject( Object[] quad )
        {
            Node subjectNode = ( Node ) quad[ 0 ];
            Uri subject = new Uri( getNodeUriOrNull( subjectNode ) );
            Uri predicate = new Uri( ( String ) quad[ 1 ] );
            Node objectNode = ( Node ) quad[ 2 ];
            Value object = getValueForObjectNode( predicate.getUriAsString(),
                objectNode );
            Node contextNode = ( Node ) quad[ 3 ];
            Context context = new Context( getNodeUriOrNull( contextNode ) );
            Relationship contextRelationship = ( Relationship ) quad[ 5 ];
            StatementMetadata metadata = new VerboseQuadStatementMetadata(
                contextRelationship );
            return object instanceof Literal ?
                new CompleteStatement( subject, predicate, ( Literal ) object,
                    context, metadata ) :
                new CompleteStatement( subject, predicate, ( Resource ) object,
                    context, metadata );
        }
    }
    
    //    private class QuadWithInferencingIterable
    //        extends NestingIterable<Object[]>
    //    {
    //        QuadWithInferencingIterable( Iterable<Object[]> quads )
    //        {
    //            super( quads );
    //        }
    //        
    //        @Override
    //        protected Iterator<Object[]> createNestedIterator( Object[] item )
    //        {
    //            return new SingleIterator<Object[]>( item );
    //        }
    //    }
    
    //    private class SingleIterator<T> extends PrefetchingIterator<T>
    //    {
    //        private T item;
    //    	
    //        SingleIterator( T item )
    //        {
    //            this.item = item;
    //        }
    //    	
    //        @Override
    //        protected T fetchNextOrNull()
    //        {
    //            T result = item;
    //            item = null;
    //            return result;
    //        }
    //    }
    
    /**
     * The Object[] will contain
     * {
     *     Node subject
     *     String predicate
     *     Node object
     *     Node context
     *     Node middleNode
     *     Relationship middleNodeToContextRelationship
     * }
     */
    private class MiddleNodeToQuadIterable implements Iterable<Object[]>
    {
        private Statement statement;
        private Iterable<Node> middleNodes;
        
        MiddleNodeToQuadIterable( Statement statement,
            Iterable<Node> middleNodes )
        {
            this.statement = statement;
            this.middleNodes = middleNodes;
        }
        
        public Iterator<Object[]> iterator()
        {
            return new MiddleNodeToQuadIterator( statement,
                middleNodes.iterator() );
        }
    }
    
    private class MiddleNodeToQuadIterator
        extends PrefetchingIterator<Object[]>
    {
        private Statement statement;
        private NestingIterator<Node, Node> middleNodesWithContexts;
        private Relationship lastContextRelationship;
        
        MiddleNodeToQuadIterator( Statement statement,
            Iterator<Node> middleNodes )
        {
            this.statement = statement;
            this.middleNodesWithContexts =
                new NestingIterator<Node, Node>( middleNodes )
            {
                @Override
                protected Iterator<Node> createNestedIterator( Node item )
                {
                    return newContextIterator( item );
                }
            };
        }
        
        @Override
        protected Object[] fetchNextOrNull()
        {
            return middleNodesWithContexts.hasNext() ? nextQuad() : null;
        }
        
        private Iterator<Node> newContextIterator( Node middleNode )
        {
            // TODO With a traverser it's... somewhat like
            // 1000 times slower, why Johan why?
            Iterator<Node> iterator = new RelationshipToNodeIterable( 
                middleNode, middleNode.getRelationships(
                    VerboseQuadStrategy.RelTypes.IN_CONTEXT,
                    Direction.OUTGOING ) )
            {
                @Override
                protected Node underlyingObjectToObject(
                    Relationship relationship )
                {
                    lastContextRelationship = relationship;
                    return super.underlyingObjectToObject( relationship );
                }
            }.iterator();
            
            if ( !statement.getContext().isWildcard() )
            {
                iterator = new FilteringIterator<Node>( iterator, new Predicate<Node>()
                {
                    public boolean accept( Node contextNode )
                    {
                        String contextUri = getNodeUriOrNull( contextNode );
                        return new Context( contextUri ).equals(
                            statement.getContext() );
                    }
                });
            }
            return iterator;
        }
        
        private Object[] nextQuad()
        {
            Node contextNode = middleNodesWithContexts.next();
            Node middleNode = middleNodesWithContexts.getCurrentSurfaceItem();
            Relationship subjectRelationship = middleNode.getRelationships(
                Direction.INCOMING ).iterator().next();
            String predicate = subjectRelationship.getType().name();
            Node subjectNode = subjectRelationship.getOtherNode( middleNode );
            try
            {
                Node objectNode = middleNode.getSingleRelationship(
                    subjectRelationship.getType(),
                    Direction.OUTGOING ).getEndNode();
                return new Object[] { subjectNode, predicate,
                    objectNode, contextNode, middleNode,
                    lastContextRelationship };
            }
            catch ( RuntimeException e )
            {
                System.out.println( "hmm, middle node " + middleNode );
                for ( Relationship rel : middleNode.getRelationships() )
                {
                    System.out.println( rel.getStartNode() + " --[" +
                        rel.getType().name() + "]--> " + rel.getEndNode() );
                }
                throw e;
            }
        }
    }
    
    private class PredicateFilteredNodes extends FilteringIterable<Node>
    {
        PredicateFilteredNodes( final Statement statement, Iterable<Node> source )
        {
            super( source, new Predicate<Node>()
            {
                public boolean accept( Node middleNode )
                {
                    Iterator<Relationship> rels = middleNode.getRelationships(
                            Direction.INCOMING ).iterator();
                    if ( !rels.hasNext() )
                    {
                        return false;
                    }
                    Relationship relationship = rels.next();
                    return relationship.getType().name().equals( ( ( Uri )
                        statement.getPredicate() ).getUriAsString() );
                }
            } );
        }
    }
    
    private class ObjectFilteredRelationships
        extends FilteringIterable<Relationship>
    {
        ObjectFilteredRelationships( final Statement statement,
                Iterable<Relationship> source )
        {
            super( source, new Predicate<Relationship>()
            {
                public boolean accept( Relationship subjectToMiddleRel )
                {
                    Node middleNode = subjectToMiddleRel.getEndNode();
                    Node objectNode = middleNode.getSingleRelationship(
                        subjectToMiddleRel.getType(), Direction.OUTGOING ).getEndNode();
                    Value objectValue = getValueForObjectNode(
                        subjectToMiddleRel.getType().name(), objectNode );
                    return objectValue.equals( statement.getObject() );
                }
            });
        }
    }
    
    //    private class SubjectFilteredRelationships
    //        extends FilteringIterable<Relationship>
    //    {
    //        private Node subjectNode;
    //        
    //        SubjectFilteredRelationships( Node subjectNode,
    //            Iterable<Relationship> source )
    //        {
    //            super( source );
    //            this.subjectNode = subjectNode;
    //        }
    //        
    //        @Override
    //        protected boolean passes( Relationship middleToObjectRel )
    //        {
    //            Node thisSubjectNode = middleToObjectRel.getStartNode().
    //            getSingleRelationship( middleToObjectRel.getType(),
    //                Direction.INCOMING ).getStartNode();
    //            return thisSubjectNode.equals( this.subjectNode );
    //        }
    //    }
    
    private class LiteralToMiddleNodeIterable
        extends IterableWrapper<Node, Node>
    {
        LiteralToMiddleNodeIterable( Iterable<Node> literalNodes )
        {
            super( literalNodes );
        }
        
        @Override
        protected Node underlyingObjectToObject( Node literalNode )
        {
            try
            {
                return literalNode.getRelationships(
                    Direction.INCOMING ).iterator().next().getStartNode();
            }
            catch ( RuntimeException e )
            {
                throw e;
            }
        }
    }
    
    private static class VerboseQuadStatementMetadata
        implements StatementMetadata
    {
        private static final String PREFIX_VALUE = "value.";
        private static final String PREFIX_DATATYPE = "datatype.";
        private static final String PREFIX_LANGUAGE = "language.";
        
        private Relationship relationship;
        
        private VerboseQuadStatementMetadata(
            Relationship relationshipBetweenMiddleNodeAndContext )
        {
            this.relationship = relationshipBetweenMiddleNodeAndContext;
        }
        
        public Literal get( String key )
        {
            Object value = relationship.getProperty( PREFIX_VALUE + key );
            String datatype = ( String )
            relationship.getProperty( PREFIX_DATATYPE + key, null );
            String language = ( String )
            relationship.getProperty( PREFIX_LANGUAGE + key, null );
            Literal literal = null;
            if ( datatype != null )
            {
                Uri datatypeUri = new Uri( datatype );
                literal = language != null ?
                    new Literal( value, datatypeUri, language ) :
                        new Literal( value, datatypeUri );
            }
            else
            {
                literal = new Literal( value );
            }
            return literal;
        }
        
        public boolean has( String key )
        {
            return relationship.hasProperty( PREFIX_VALUE + key );
        }
        
        public void remove( String key )
        {
            relationship.removeProperty( PREFIX_VALUE + key );
            setOrRemoveIfExists( PREFIX_DATATYPE + key, null );
            setOrRemoveIfExists( PREFIX_LANGUAGE + key, null );
        }
        
        public void set( String key, Literal value )
        {
            Object literalValue = value.getValue();
            Uri datatypeUri = value.getDatatype();
            String language = value.getLanguage();
            relationship.setProperty( PREFIX_VALUE + key, literalValue );
            setOrRemoveIfExists( PREFIX_DATATYPE + key,
                datatypeUri != null ? datatypeUri.getUriAsString() : null );
            setOrRemoveIfExists( PREFIX_LANGUAGE + key, language );
        }
        
        private void setOrRemoveIfExists( String key, Object value )
        {
            if ( value != null )
            {
                relationship.setProperty( key, value );
            }
            else if ( relationship.hasProperty( key ) )
            {
                relationship.removeProperty( key );
            }
        }
        
        public Iterable<String> getKeys()
        {
            Collection<String> keys = new ArrayList<String>();
            for ( String key : relationship.getPropertyKeys() )
            {
                if ( key.startsWith( PREFIX_VALUE ) )
                {
                    keys.add( key.substring( PREFIX_VALUE.length() ) );
                }
            }
            return keys;
        }
    }
    
    public class QuadVerificationHook implements VerificationHook
    {
        private static final int INTERVAL = 10000;
        private PrintStream output;
        private int max;
        private int counter;
        
        public Status verify( long id, String predicate, Object literal )
        {
            incrementCounter();
            Status result = Status.OK;
            try
            {
                Node node = graphDb().getNodeById( id );
                Value value = getValueForObjectNode( predicate, node );
                if ( !( value instanceof Literal ) )
                {
                    result = Status.NOT_LITERAL;
                }
                else
                {
                    Literal literalObject = ( Literal ) value;
                    if ( !literalObject.getValue().toString().equals(
                        literal.toString() ) )
                    {
                        result = Status.WRONG_LITERAL;
                    }
                }
            }
            catch ( NotFoundException e )
            {
                result = Status.MISSING;
            }
            
            if ( result != Status.OK )
            {
                output.println( result.name() + " " + id );
            }
            return result;
        }

        public void verificationCompleted( Map<Status, Integer> counts )
        {
            displayProgress();
            output.println( "\n-----------------\nTotal\n" );
            for ( Map.Entry<Status, Integer> entry : counts.entrySet() )
            {
                output.println( entry.getKey().name() + ": " +
                    entry.getValue() );
            }
        }

        public void verificationStarting( int numberOfDocumentsToVerify )
        {
            try
            {
                output = new PrintStream( new File( "verify-fulltextindex-" +
                    System.currentTimeMillis() ) );
                output.println( "Verification starting. We have " +
                    numberOfDocumentsToVerify + " documents ahead of us,\n" +
                    "displaying progress each " + INTERVAL + " records\n" );
                this.max = numberOfDocumentsToVerify;
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        
        private void incrementCounter()
        {
            if ( ++counter % INTERVAL == 0 )
            {
                displayProgress();
            }
        }
        
        private void displayProgress()
        {
            double percent = ( double ) counter / ( double ) max;
            percent *= 100d;
            output.println( "---" + counter +
                " (" + ( int ) percent + "%)" );
        }

        public void oneWasSkipped()
        {
            incrementCounter();
        }
    }
}
