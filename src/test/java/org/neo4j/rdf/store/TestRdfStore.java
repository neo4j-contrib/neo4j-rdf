package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureImpl;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.RepresentationStrategy;
import org.neo4j.rdf.store.representation.standard.DenseRepresentationStrategy;
import org.neo4j.rdf.store.representation.standard.MetaEnabledUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseRepresentationStrategy;

/**
 * Tests an {@link RdfStore}.
 */
public class TestRdfStore extends StoreTestCase
{
    /**
     * Tests an {@link RdfStore} with a {@link DenseRepresentationStrategy}.
     * @throws Exception if there's an error in the test.
     */
	public void testDense() throws Exception
	{
		RdfStore store = new RdfStoreImpl( neo(),
			new DenseRepresentationStrategy( neo() ) );
		applyAndRemoveStatements( store );
		deleteEntireNodeSpace();
	}
	
    /**
     * Tests an {@link RdfStore} with a {@link VerboseRepresentationStrategy}.
     * @throws Exception if there's an error in the test.
     */
	public void testVerbose() throws Exception
	{
		RdfStore store = new RdfStoreImpl( neo(),
			new VerboseRepresentationStrategy( neo() ) );
		applyAndRemoveStatements( store );
		deleteEntireNodeSpace();
	}
	
    /**
     * Tests an {@link RdfStore} with a {@link VerboseRepresentationStrategy}
     * with a {@link MetaStructure}.
     * @throws Exception if there's an error in the test.
     */
	public void testVerboseMeta() throws Exception
	{
	    MetaStructure meta = new MetaStructureImpl( neo() );
	    RepresentationStrategy strategy = new VerboseRepresentationStrategy(
	        neo(), meta );
	    RdfStore store = new RdfStoreImpl( neo(), strategy );
	    MetaStructureClass personClass =
	        meta.getGlobalNamespace().getMetaClass( PERSON_CLASS, true );
        meta.getGlobalNamespace().getMetaProperty( NAME_PROPERTY, true );
        MetaStructureProperty knowsProperty =
            meta.getGlobalNamespace().getMetaProperty( KNOWS_PROPERTY, true );
	    List<Statement> statements = applyStatements( store );
	    
	    // Verify
	    Node knowsPropertyNode = knowsProperty.node();
	    assertEquals( 3, personClass.getInstances().size() );
	    assertEquals( 2, countIterable( knowsPropertyNode.getRelationships(
	        VerboseRepresentationStrategy.RelTypes.
	            CONNECTOR_HAS_PREDICATE ) ) );
	    
	    removeStatements( store, statements );
	    deleteEntireNodeSpace();
	}
	
	private List<Statement> applyStatements( RdfStore store )
	{
	    String typePredicate = MetaEnabledUriBasedExecutor.RDF_TYPE_URI;
	    Uri personClass = new Uri( PERSON_CLASS );
		String subject = "http://henrik";
		String otherSubject = "http://emil";
		String thirdSubject = "http://mattias";
		String namePredicate = NAME_PROPERTY;
		String knowsPredicate = KNOWS_PROPERTY;
		Object object = "Henrik";
		Object otherObject = "Emil";
		Object thirdObject = "Mattias";
		
		Statement subjectTypeStatement =
		    statement( subject, typePredicate, personClass );
		addTwice( store, subjectTypeStatement );
		Statement subjectNameStatement =
			statement( subject, namePredicate, object );
		addTwice( store, subjectNameStatement );
		
        Statement otherSubjectTypeStatement =
            statement( otherSubject, typePredicate, personClass );
        addTwice( store, otherSubjectTypeStatement );
		Statement otherSubjectNameStatement =
		    statement( otherSubject, namePredicate, otherObject );
		addTwice( store, otherSubjectNameStatement );
		
		Statement thirdSubjectNameStatement =
		    statement( thirdSubject, namePredicate, thirdObject );
		addTwice( store, thirdSubjectNameStatement );
        Statement thirdSubjectTypeStatement =
            statement( thirdSubject, typePredicate, personClass );
        addTwice( store, thirdSubjectTypeStatement );
		
		Statement knowsStatement =
		    statement( subject, knowsPredicate, new Uri( otherSubject ) );
		addTwice( store, knowsStatement );
		Statement otherKnowsStatement =
		    statement( subject, knowsPredicate, new Uri( thirdSubject ) );
		addTwice( store, otherKnowsStatement );
		
		return new ArrayList<Statement>(
		    Arrays.asList(
		        subjectTypeStatement,
		        subjectNameStatement,
		        otherSubjectTypeStatement,
		        otherSubjectNameStatement,
		        thirdSubjectTypeStatement,
		        thirdSubjectNameStatement,
		        knowsStatement,
		        otherKnowsStatement ) );
	}
	
	private void applyAndRemoveStatements( RdfStore store )
	{
	    removeStatements( store, applyStatements( store ) );
	}
}
