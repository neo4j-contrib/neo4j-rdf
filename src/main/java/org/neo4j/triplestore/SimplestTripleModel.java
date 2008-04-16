package org.neo4j.triplestore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.util.NeoPropertyArraySet;
import org.neo4j.util.PureNodeRelationshipSet;

/**
 * (S) ---(P)---> (O)
 * The relationship type will be the name of the predicate.
 */
public class SimplestTripleModel implements TripleModel
{
	private NeoService neo;
	
	/**
	 * @param neo the {@link NeoService}.
	 */
	public SimplestTripleModel( NeoService neo )
	{
		this.neo = neo;
	}
	
	protected Collection<Object> getValues( Node node, String key )
	{
		return new NeoPropertyArraySet<Object>( neo, node, key );
	}
	
	private void debug( String message )
	{
//		System.out.println( message );
	}
	
	public void connect( Node subject, String predicate, Object dataValue )
	{
		boolean added = getValues( subject, predicate ).add( dataValue );
		if ( added )
		{
			debug( subject + " + *" + predicate + " " + dataValue );
		}
	}
	
	public void connect( Node subject, String predicate, Node object )
	{
		boolean added = new PureNodeRelationshipSet( subject,
			ModelRelType.get( predicate ) ).add( object );
		if ( added )
		{
			debug( subject + " + " + predicate + " -> " + object );
		}
	}
	
	public void disconnect( Node subject, String predicate, Object dataValue )
	{
		boolean removed = getValues( subject, predicate ).remove( dataValue );
		if ( removed )
		{
			debug( subject + " - *" + predicate + " " + dataValue );
		}
	}
	
	public void disconnect( Node subject, String predicate, Node object )
	{
		boolean removed = new PureNodeRelationshipSet( subject,
			ModelRelType.get( predicate ) ).remove( object );
		if ( removed )
		{
			debug( subject + " - " + predicate + " -> " + object );
		}
	}
	
	private static class ModelRelType implements RelationshipType
	{
		private static Map<String, RelationshipType> types =
			new HashMap<String, RelationshipType>();
		
		private String name;
		
		private ModelRelType( String name )
		{
			this.name = name;
		}
		
		public String name()
		{
			return this.name;
		}
		
		static RelationshipType get( String name )
		{
			synchronized ( types )
			{
				RelationshipType result = types.get( name );
				if ( result == null )
				{
					result = new ModelRelType( name );
					types.put( name, result );
				}
				return result;
			}
		}
	}
}
