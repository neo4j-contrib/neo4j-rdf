package org.neo4j.triplestore;

import java.net.URI;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.util.NeoUtil;
import org.neo4j.util.index.Index;
import org.neo4j.util.index.SingleValueIndex;

/**
 * Neo implementation of {@link TripleStore}.
 */
public class NeoTripleStore implements TripleStore
{
	private TripleModel model;
	private NeoUtil neoUtil;
	private Index instanceIndex;
	
	/**
	 * @param neo the {@link NeoService}.
	 * @param model the model to use.
	 */
	public NeoTripleStore( NeoService neo, TripleModel model )
	{
		this.model = model;
		this.neoUtil = new NeoUtil( neo );
		this.instanceIndex = newIndex();
	}
	
	private Index newIndex()
	{
		Transaction tx = neo().beginTx();
		try
		{
			Node node = neoUtil.getOrCreateSubReferenceNode(
				TripleStoreRelTypes.REF_TRIPLE_STORE_INSTANCE_INDEX );
			Index index =
				new SingleValueIndex( "triplestore", node, neo() );
			tx.success();
			return index;
		}
		finally
		{
			tx.finish();
		}
	}
	
	protected TripleModel model()
	{
		return this.model;
	}
	
	protected NeoService neo()
	{
		return neoUtil.neo();
	}
	
	protected Node getNodeByUri( URI uri )
	{
		String key = uri.toString();
		Node node = this.instanceIndex.getSingleNodeFor( key );
		if ( node == null )
		{
			node = neo().createNode();
			this.instanceIndex.index( node, key );
		}
		return node;
	}
	
	protected Object datatypeObjectToRealObject( Node subjectNode,
		String predicate, String object )
	{
		return object;
	}
	
	public void writeStatement( URI subject, String predicate,
		String object )
	{
		changeStatement( subject, predicate, object, true );
	}
	
	public void writeStatement( URI subject, String predicate,
		URI objectUri )
	{
		changeStatement( subject, predicate, objectUri, true );
	}

	public void deleteStatement( URI subject, String predicate,
		String object )
	{
		changeStatement( subject, predicate, object, false );
	}
	
	public void deleteStatement( URI subject, String predicate,
		URI objectUri )
	{
		changeStatement( subject, predicate, objectUri, false );
	}
	
	private void changeStatement( URI subject, String predicate,
		String object, boolean connect )
	{
		Transaction tx = neo().beginTx();
		try
		{
			Node subjectNode = getNodeByUri( subject );
			Object realValue = datatypeObjectToRealObject(
				subjectNode, predicate, object );
			if ( connect )
			{
				model.connect( subjectNode, predicate, realValue );
			}
			else
			{
				model.disconnect( subjectNode, predicate, realValue );
			}
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	protected Node getObjectNode( String predicate, URI objectUri )
	{
		return getNodeByUri( objectUri );
	}
	
	private void changeStatement( URI subject, String predicate,
		URI objectUri, boolean connect )
	{
		Transaction tx = neo().beginTx();
		try
		{
			Node subjectNode = getNodeByUri( subject );
			Node objectNode = getObjectNode( predicate, objectUri );
			if ( connect )
			{
				model.connect( subjectNode, predicate, objectNode );
			}
			else
			{
				model.disconnect( subjectNode, predicate, objectNode );
			}
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}

	private static enum TripleStoreRelTypes implements RelationshipType
	{
		/**
		 * The instance index root node.
		 */
		REF_TRIPLE_STORE_INSTANCE_INDEX,
	}
}
