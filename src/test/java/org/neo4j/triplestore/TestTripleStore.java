package org.neo4j.triplestore;

import java.net.URI;

import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureImpl;

/**
 * Tests the triple store.
 */
public class TestTripleStore extends NeoTestCase
{
	/**
	 * Some basic tests.
	 * @throws Exception
	 */
	public void testIt() throws Exception
	{
		TripleModel model = new SimplestTripleModel( neo() );
		TripleStore store = new NeoTripleStore( neo(), model );
		
		String namespace = "http://neo4j.org/triplestore#";
		String name = namespace + "name";
		String nickName = namespace + "nickName";
		String associatedWith = namespace + "associatedWith";
		URI object1 = new URI( namespace + "object1" );
		URI object2 = new URI( namespace + "object2" );
		
		store.writeStatement( object1, name, "Mattias" );
		store.writeStatement( object1, nickName, "Matte" );
		store.writeStatement( object1, nickName, "Mathew" );
		store.writeStatement( object1, associatedWith, object2 );
		store.writeStatement( object2, name, "Emil" );

		store.deleteStatement( object1, name, "Mattias" );
		store.deleteStatement( object1, nickName, "Matte" );
		store.deleteStatement( object1, nickName, "Mathew" );
		store.deleteStatement( object1, associatedWith, object2 );
		store.deleteStatement( object2, name, "Emil" );
		
		deleteEntireNodeSpace();
	}
	
	/**
	 * Test triple stores with a meta model.
	 * @throws Exception
	 */
	public void testWithMeta() throws Exception
	{
		MetaStructure meta = new MetaStructureImpl( neo() );
		String baseUri = "http://test.org/test#";
		String personUri = baseUri + "Person";
		String nameUri = baseUri + "name";
		MetaStructureClass person =
			meta.getGlobalNamespace().getMetaClass( personUri, true );
		meta.getGlobalNamespace().getMetaProperty( nameUri, true );
		
		TripleStore store = new NeoMetaTripleStore( neo(), meta,
			new VerboseMetaTripleModel( neo(), meta ) );
		URI mathew = new URI( baseUri + "mathew" );
		try
		{
			store.writeStatement( mathew, NeoMetaTripleStore.RDF_TYPE_URI,
				new URI( baseUri + "Something" ) );
			fail( "Shouldn't accept strange types" );
		}
		catch ( Exception e )
		{ // Good
		}
		
		assertEquals( 0, person.getInstances().size() );
		store.writeStatement( mathew, NeoMetaTripleStore.RDF_TYPE_URI,
			new URI( personUri ) );
		assertEquals( 1, person.getInstances().size() );
		store.writeStatement( mathew, nameUri, "Mattias" );
		assertEquals( 1, person.getInstances().size() );
		store.deleteStatement( mathew, nameUri, "Mattias" );
		store.deleteStatement( mathew, NeoMetaTripleStore.RDF_TYPE_URI,
			new URI( personUri ) );
		assertEquals( 0, person.getInstances().size() );
		deleteEntireNodeSpace();
	}
}
