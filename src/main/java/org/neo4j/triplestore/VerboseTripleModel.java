package org.neo4j.triplestore;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.util.NeoPropertyArraySet;

/**
 * Uses a node in between each connection which has relationships to the
 * subject, predicate and object.
 */
public abstract class VerboseTripleModel implements TripleModel
{
	private NeoService neo;
	
	/**
	 * @param neo the {@link NeoService}.
	 */
	public VerboseTripleModel( NeoService neo )
	{
		this.neo = neo;
	}
	
	protected NeoService neo()
	{
		return this.neo;
	}
	
	protected abstract Node getPredicateNode( String predicate );
	
	public void connect( Node subject, String predicate, Object dataValue )
	{
		new NeoPropertyArraySet<Object>( neo(),
			subject, predicate ).add( dataValue );
	}
	
	public void connect( Node subject, String predicate, Node object )
	{
		Node connector = neo().createNode();
		subject.createRelationshipTo( connector,
			RelTypes.SUBJECT_TO_CONNECTOR );
		connector.createRelationshipTo( getPredicateNode( predicate ),
			RelTypes.CONNECTOR_TO_PREDICATE );
		connector.createRelationshipTo( object, RelTypes.CONNECTOR_TO_OBJECT );
	}
	
	public void disconnect( Node subject, String predicate, Object dataValue )
	{
		new NeoPropertyArraySet<Object>( neo(),
			subject, predicate ).remove( dataValue );
	}
	
	public void disconnect( Node subject, String predicate, Node object )
	{
		Node predicateNode = getPredicateNode( predicate );
		for ( Relationship rel : subject.getRelationships(
			RelTypes.SUBJECT_TO_CONNECTOR, Direction.OUTGOING ) )
		{
			Node connector = rel.getOtherNode( subject );
			Relationship objectRel = connector.getSingleRelationship(
				RelTypes.CONNECTOR_TO_OBJECT, Direction.OUTGOING );
			Relationship predicateRel = connector.getSingleRelationship(
				RelTypes.CONNECTOR_TO_PREDICATE, Direction.OUTGOING );
			if ( objectRel.getOtherNode( connector ).equals( object ) &&
				predicateRel.getOtherNode( connector ).equals( predicateNode ) )
			{
				objectRel.delete();
				predicateRel.delete();
				rel.delete();
				connector.delete();
				break;
			}
		}
	}
	
	/**
	 * The relationship types used internally.
	 */
	public static enum RelTypes implements RelationshipType
	{
		/**
		 * The connector node to the subject.
		 */
		SUBJECT_TO_CONNECTOR,
		
		/**
		 * The connector node to the predicate.
		 */
		CONNECTOR_TO_PREDICATE,
		
		/**
		 * The connector node to the object.
		 */
		CONNECTOR_TO_OBJECT,
	}
}
