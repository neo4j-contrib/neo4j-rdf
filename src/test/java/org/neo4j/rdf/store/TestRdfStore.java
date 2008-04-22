package org.neo4j.rdf.store;

import org.neo4j.rdf.model.Predicate;
import org.neo4j.rdf.model.PredicateImpl;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.StatementImpl;
import org.neo4j.rdf.model.Subject;
import org.neo4j.rdf.model.SubjectImpl;
import org.neo4j.rdf.model.TripleObject;
import org.neo4j.rdf.model.TripleObjectLiteral;
import org.neo4j.rdf.model.TripleObjectResource;
import org.neo4j.rdf.store.representation.DenseRepresentationStrategy;
import org.neo4j.rdf.store.representation.VerboseRepresentationStrategy;

/**
 * Tests an {@link RdfStore}.
 */
public class TestRdfStore extends NeoTestCase
{
    /**
     * Tests an {@link RdfStore} with a {@link DenseRepresentationStrategy}.
     * @throws Exception if there's an error in the test.
     */
	public void testDense() throws Exception
	{
		RdfStore store = new RdfStoreImpl( neo(),
			new DenseRepresentationStrategy( neo() ) );
		applyStatements( store );
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
		applyStatements( store );
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

	private void remove( RdfStore store, Statement statement, int numberOfTimes )
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

	private void applyStatements( RdfStore store )
	{
		Subject subject = new SubjectImpl( "http://henrik" );
		Subject otherSubject = new SubjectImpl( "http://emil" );
		Subject thirdSubject = new SubjectImpl( "http://mattias" );
		Predicate namePredicate = new PredicateImpl( "http://name" );
		Predicate knowsPredicate = new PredicateImpl( "http://knows" );
		TripleObject object = new TripleObjectLiteral( "Henrik" );
		TripleObject otherObject = new TripleObjectLiteral( "Emil" );
		TripleObject thirdObject = new TripleObjectLiteral( "Mattias" );
		
		Statement subjectNameStatement =
			new StatementImpl( subject, namePredicate, object );
		addTwice( store, subjectNameStatement );
		Statement otherSubjectNameStatement = new StatementImpl( otherSubject,
			namePredicate, otherObject );
		addTwice( store, otherSubjectNameStatement );
		Statement thirdSubjectNameStatement = new StatementImpl( thirdSubject,
			namePredicate, thirdObject );
		addTwice( store, thirdSubjectNameStatement );
		Statement knowsStatement = new StatementImpl( subject, knowsPredicate,
			new TripleObjectResource( otherSubject ) );
		addTwice( store, knowsStatement );
		Statement otherKnowsStatement = new StatementImpl( subject,
			knowsPredicate, new TripleObjectResource( thirdSubject ) );
		addTwice( store, otherKnowsStatement );
		
		removeTwice( store, subjectNameStatement );
		removeTwice( store, thirdSubjectNameStatement );
		removeTwice( store, otherKnowsStatement );
		removeTwice( store, otherSubjectNameStatement );
		removeTwice( store, knowsStatement );
	}
}
