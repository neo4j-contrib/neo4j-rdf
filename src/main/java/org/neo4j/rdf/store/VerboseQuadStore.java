package org.neo4j.rdf.store;

import java.util.Iterator;
import java.util.LinkedList;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.Transaction;
import org.neo4j.api.core.TraversalPosition;
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
import org.neo4j.util.IterableWrapper;
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
        debug( "getStatements() in: " + statement );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
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
    	return new MiddleNodeToStatementIterable( statement, middleNodes );
	}
    
    private class MiddleNodeToStatementIterable
    	implements Iterable<CompleteStatement>
    {
    	private Statement statement;
    	private Iterable<Node> middleNodes;
    	
    	MiddleNodeToStatementIterable( Statement statement,
    		Iterable<Node> middleNodes )
		{
    		this.statement = statement;
    		this.middleNodes = middleNodes;
		}
    	
		public Iterator<CompleteStatement> iterator()
        {
			return new MiddleNodeToStatementIterator( statement,
				middleNodes.iterator() );
        }
    }
    
    private class MiddleNodeToStatementIterator
    	extends PrefetchingIterator<CompleteStatement>
    {
    	private Iterator<Node> middleNodes;
    	private ContextMatcherRE contextMatcher;
    	
    	// They are both null or both non-null synced.
    	private Node currentMiddleNode;
    	private Iterator<Node> currentMiddleNodeContexts;
    	
    	MiddleNodeToStatementIterator( Statement statement,
    		Iterator<Node> middleNodes )
    	{
    		this.middleNodes = middleNodes;
    		this.contextMatcher = new ContextMatcherRE( statement );
    	}
    	
		@Override
        protected CompleteStatement fetchNextOrNull()
        {
			if ( currentMiddleNodeContexts == null ||
				!currentMiddleNodeContexts.hasNext() )
			{
				while ( middleNodes.hasNext() )
				{
					currentMiddleNode = middleNodes.next();
					currentMiddleNodeContexts = currentMiddleNode.traverse(
						Order.BREADTH_FIRST, StopEvaluator.END_OF_NETWORK,
						contextMatcher, VerboseQuadStrategy.RelTypes.IN_CONTEXT,
						Direction.OUTGOING ).iterator();
					if ( currentMiddleNodeContexts.hasNext() )
					{
						break;
					}
				}
			}

			if ( currentMiddleNodeContexts != null &&
				currentMiddleNodeContexts.hasNext() )
			{
				return newStatement();
			}
			return null;
        }
		
		private CompleteStatement newStatement()
		{
			Node middleNode = currentMiddleNode;
			Relationship subjectRelationship = middleNode.getRelationships(
				Direction.INCOMING ).iterator().next();
			Node subjectNode = subjectRelationship.getOtherNode( middleNode );
			Uri subject = new Uri( getNodeUriOrNull( subjectNode ) );
			Uri predicate = new Uri( subjectRelationship.getType().name() );
			
			Node objectNode = middleNode.getSingleRelationship(
				subjectRelationship.getType(),
					Direction.OUTGOING ).getEndNode();
			Value object = getValueForObjectNode( predicate.getUriAsString(),
				objectNode );
			
			Node contextNode = currentMiddleNodeContexts.next();
			Context context = new Context( getNodeUriOrNull( contextNode ) );
			
			return object instanceof Literal ?
				new CompleteStatement( subject, predicate, ( Literal ) object,
					context ) :
				new CompleteStatement( subject, predicate, ( Resource ) object,
					context );
		}
    }
    
    private class ContextMatcherRE implements ReturnableEvaluator
    {
    	private Statement statement;
    	
    	ContextMatcherRE( Statement statement )
    	{
    		this.statement = statement;
    	}
    	
		public boolean isReturnableNode( TraversalPosition position )
        {
			if ( position.depth() == 0 )
			{
				// This would be the starting node.
				return false;
			}
			
			Value statementContext = statement.getContext();
			if ( statementContext.isWildcard() )
			{
				return true;
			}
			else
			{
				String contextUri = getNodeUriOrNull( position.currentNode() );
				return new Context( contextUri ).equals( statementContext );
			}
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
