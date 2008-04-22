package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureImpl;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.rdf.model.Predicate;
import org.neo4j.rdf.model.PredicateImpl;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.StatementImpl;
import org.neo4j.rdf.model.Subject;
import org.neo4j.rdf.model.SubjectImpl;
import org.neo4j.rdf.model.TripleObject;
import org.neo4j.rdf.model.TripleObjectLiteral;
import org.neo4j.rdf.model.TripleObjectResource;
import org.neo4j.rdf.model.UriImpl;
import org.neo4j.rdf.store.representation.DenseRepresentationStrategy;
import org.neo4j.rdf.store.representation.RdfRepresentationStrategy;
import org.neo4j.rdf.store.representation.VerboseRepresentationStrategy;

/**
 * Tests an {@link RdfStore}.
 */
public class TestRdfStore extends NeoTestCase
{
    private static final String PERSON_CLASS = "http://classes#Person";
    private static final String NAME_PROPERTY = "http://properties#name";
    private static final String KNOWS_PROPERTY = "http://properties#knows";
    
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
	    RdfRepresentationStrategy strategy = new VerboseRepresentationStrategy(
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
	
	private void add( RdfStore store, Statement statement, int numberOfTimes )
	{
		while ( numberOfTimes-- > 0 )
		{
			store.addStatement( statement );
		}
	}
	
	private void addTwice( RdfStore store, Statement statement )
	{
		add( store, statement, 2 );
	}

	private void remove( RdfStore store, Statement statement,
	    int numberOfTimes )
	{
		while ( numberOfTimes-- > 0 )
		{
			store.removeStatements( statement );
		}
	}
	
	private void removeTwice( RdfStore store, Statement statement )
	{
		remove( store, statement, 2 );
	}

	private List<Statement> applyStatements( RdfStore store )
	{
	    Predicate typePredicate = new PredicateImpl(
	        MetaEnabledAsrExecutor.RDF_TYPE_URI );
	    TripleObject personClass = new TripleObjectResource(
	        new UriImpl( PERSON_CLASS ) );
		Subject subject = new SubjectImpl( "http://henrik" );
		Subject otherSubject = new SubjectImpl( "http://emil" );
		Subject thirdSubject = new SubjectImpl( "http://mattias" );
		Predicate namePredicate = new PredicateImpl( NAME_PROPERTY );
		Predicate knowsPredicate = new PredicateImpl( KNOWS_PROPERTY );
		TripleObject object = new TripleObjectLiteral( "Henrik" );
		TripleObject otherObject = new TripleObjectLiteral( "Emil" );
		TripleObject thirdObject = new TripleObjectLiteral( "Mattias" );
		
		Statement subjectTypeStatement =
		    new StatementImpl( subject, typePredicate, personClass );
		addTwice( store, subjectTypeStatement );
		Statement subjectNameStatement =
			new StatementImpl( subject, namePredicate, object );
		addTwice( store, subjectNameStatement );
		
        Statement otherSubjectTypeStatement =
            new StatementImpl( otherSubject, typePredicate, personClass );
        addTwice( store, otherSubjectTypeStatement );
		Statement otherSubjectNameStatement = new StatementImpl( otherSubject,
			namePredicate, otherObject );
		addTwice( store, otherSubjectNameStatement );
		
		Statement thirdSubjectNameStatement = new StatementImpl( thirdSubject,
			namePredicate, thirdObject );
		addTwice( store, thirdSubjectNameStatement );
        Statement thirdSubjectTypeStatement =
            new StatementImpl( thirdSubject, typePredicate, personClass );
        addTwice( store, thirdSubjectTypeStatement );
		
		Statement knowsStatement = new StatementImpl( subject, knowsPredicate,
			new TripleObjectResource( otherSubject ) );
		addTwice( store, knowsStatement );
		Statement otherKnowsStatement = new StatementImpl( subject,
			knowsPredicate, new TripleObjectResource( thirdSubject ) );
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
	
	private void removeStatements( RdfStore store, List<Statement> statements )
	{
        while ( !statements.isEmpty() )
        {
            Statement statement = statements.remove(
                new Random().nextInt( statements.size() ) );
            removeTwice( store, statement );
        }
	}
	
	private void applyAndRemoveStatements( RdfStore store )
	{
	    removeStatements( store, applyStatements( store ) );
	}
}
