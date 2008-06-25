package org.neo4j.rdf.store;

import java.util.Iterator;
import java.util.LinkedList;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.Transaction;
import org.neo4j.api.core.Traverser.Order;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseQuadExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseQuadStrategy;
import org.neo4j.util.FilteringIterable;
import org.neo4j.util.FilteringIterator;
import org.neo4j.util.IterableWrapper;
import org.neo4j.util.NestingIterable;
import org.neo4j.util.NestingIterator;
import org.neo4j.util.OneOfRelTypesReturnableEvaluator;
import org.neo4j.util.PrefetchingIterator;
import org.neo4j.util.RelationshipToNodeIterable;
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
        debug( "I'm initialized!" );
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
//        debug( "getStatements( " + statement + " )" );
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
    
    @Override
    public int size( Context... contexts )
    {
    	Transaction tx = neo().beginTx();
    	try
    	{
    		int size = 0;
    		Node contextRefNode = getRepresentationStrategy().getExecutor().
				getContextsReferenceNode();
    		for ( Relationship rel : contextRefNode.getRelationships(
    			VerboseQuadExecutor.RelTypes.IS_A_CONTEXT,
    			Direction.OUTGOING ) )
    		{
    			Node contextNode = rel.getOtherNode( contextRefNode );
    			size += ( Integer ) contextNode.getProperty(
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

    private Node lookupNode( Value uri )
    {
        return getRepresentationStrategy().getExecutor().
            lookupNode( new AbstractNode( uri ) );
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
            Object value = objectNode.getProperty( predicate );
            String datatype = ( String ) objectNode.getProperty(
                VerboseQuadExecutor.LITERAL_DATATYPE_KEY, null );
            String language = ( String ) objectNode.getProperty(
                VerboseQuadExecutor.LITERAL_LANGUAGE_KEY, null );
            return new Literal( value, datatype == null ? null :
                new Uri( datatype ), language );
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
    
    private RelationshipType relType( Value value )
    {
    	return relType( ( ( Uri ) value ).getUriAsString() );
    }
    
    private RelationshipType relType( Statement statement )
    {
    	return relType( statement.getPredicate() );
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
		return getRepresentationStrategy().getExecutor().
			getContextsReferenceNode().traverse( Order.DEPTH_FIRST,
				StopEvaluator.END_OF_NETWORK,
				new OneOfRelTypesReturnableEvaluator(
					VerboseQuadStrategy.RelTypes.IN_CONTEXT ),
				VerboseQuadExecutor.RelTypes.IS_A_CONTEXT, Direction.OUTGOING,
				VerboseQuadStrategy.RelTypes.IN_CONTEXT, Direction.INCOMING );
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
//    	int subjectEnergy = ( Integer ) subjectNode.getProperty(
//    		VerboseQuadExecutor.SUBJECT_ENERGY, 0 );
//    	int objectEnergy = 0;
//    	Node objectNode = null;
//    	if ( statement.getObject() instanceof Uri )
//    	{
//        	objectNode = lookupNode( statement.getObject() );
//        	if ( objectNode == null )
//        	{
//        		return null;
//        	}
//        	objectEnergy = ( Integer ) objectNode.getProperty(
//        		VerboseQuadExecutor.OBJECT_ENERGY, 0 );
//    	}
//    	
//    	if ( objectNode == null || subjectEnergy > objectEnergy )
//    	{
	    	Iterable<Relationship> relationships =
	    		subjectNode.getRelationships( Direction.OUTGOING );
	    	relationships = new ObjectFilteredRelationships( statement,
	    		relationships );
	    	Iterable<Node> middleNodes = new RelationshipToNodeIterable(
	    		subjectNode, relationships );
	    	return statementIterator( statement, middleNodes );
//    	}
//    	else
//    	{
//	    	Iterable<Relationship> relationships =
//	    		objectNode.getRelationships( Direction.INCOMING );
//	    	relationships = new SubjectFilteredRelationships( subjectNode,
//	    		relationships );
//	    	Iterable<Node> middleNodes = new RelationshipToNodeIterable(
//	    		subjectNode, relationships );
//	    	return statementIterator( statement, middleNodes );
//    	}
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
    		// TODO Slow
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
    		// TODO Slow
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
			return object instanceof Literal ?
				new CompleteStatement( subject, predicate, ( Literal ) object,
					context ) :
				new CompleteStatement( subject, predicate, ( Resource ) object,
					context );
        }
    }
    
    private class QuadWithInferencingIterable
    	extends NestingIterable<Object[]>
    {
    	QuadWithInferencingIterable(
    		Iterable<Object[]> quads )
		{
    		super( quads );
		}
    	
		@Override
        protected Iterator<Object[]> createNestedIterator( Object[] item )
        {
        	return new SingleIterator<Object[]>( item );
        }
    }
    
    private class SingleIterator<T> extends PrefetchingIterator<T>
    {
    	private T item;
    	
    	SingleIterator( T item )
    	{
    		this.item = item;
    	}
    	
		@Override
        protected T fetchNextOrNull()
        {
			T result = item;
			item = null;
			return result;
        }
    }
    
    /**
     * The Object[] will contain
     * {
     * 		Node subject
     * 		String predicate
     * 		Node object
     * 		Node context
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
    	private NestingIterator<Node> middleNodesWithContexts;
    	
    	MiddleNodeToQuadIterator( Statement statement,
    		Iterator<Node> middleNodes )
    	{
    		this.statement = statement;
    		this.middleNodesWithContexts =
    			new NestingIterator<Node>( middleNodes )
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
					Direction.OUTGOING ) ).iterator();
			if ( !statement.getContext().isWildcard() )
			{
				iterator = new FilteringIterator<Node>( iterator )
				{
					@Override
                    protected boolean passes( Node contextNode )
                    {
						String contextUri = getNodeUriOrNull( contextNode );
						return new Context( contextUri ).equals(
							statement.getContext() );
                    }
				};
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
			Node objectNode = middleNode.getSingleRelationship(
				subjectRelationship.getType(),
					Direction.OUTGOING ).getEndNode();
			return new Object[] { subjectNode, predicate,
				objectNode, contextNode };
		}
    }
    
    private class PredicateFilteredNodes
    	extends FilteringIterable<Node>
    {
    	private Statement statement;
    	
    	PredicateFilteredNodes( Statement statment, Iterable<Node> source )
    	{
    		super( source );
    		this.statement = statment;
    	}
    	
		@Override
        protected boolean passes( Node middleNode )
        {
			Relationship relationship = middleNode.getRelationships(
				Direction.INCOMING ).iterator().next();
			return relationship.getType().name().equals( ( ( Uri )
				statement.getPredicate() ).getUriAsString() );
        }
    }
    
    private class ObjectFilteredRelationships
    	extends FilteringIterable<Relationship>
    {
    	private Statement statement;
    	
    	ObjectFilteredRelationships( Statement statement,
    		Iterable<Relationship> source )
    	{
    		super( source );
    		this.statement = statement;
    	}
    	
		@Override
        protected boolean passes( Relationship subjectToMiddleRel )
        {
			Node middleNode = subjectToMiddleRel.getEndNode();
			Node objectNode = middleNode.getSingleRelationship(
				subjectToMiddleRel.getType(), Direction.OUTGOING ).getEndNode();
			Value objectValue = getValueForObjectNode(
				subjectToMiddleRel.getType().name(), objectNode );
			return objectValue.equals( statement.getObject() );
        }
    }
    
    private class SubjectFilteredRelationships
    	extends FilteringIterable<Relationship>
    {
    	private Node subjectNode;
    	
    	SubjectFilteredRelationships( Node subjectNode,
    		Iterable<Relationship> source )
    	{
    		super( source );
    		this.subjectNode = subjectNode;
    	}
    	
    	@Override
    	protected boolean passes( Relationship middleToObjectRel )
    	{
    		Node thisSubjectNode = middleToObjectRel.getStartNode().
    			getSingleRelationship( middleToObjectRel.getType(),
    				Direction.INCOMING ).getStartNode();
    		return thisSubjectNode.equals( this.subjectNode );
    	}
    }
    
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
	        return literalNode.getRelationships(
	        	Direction.INCOMING ).iterator().next().getStartNode();
        }
    }
}
