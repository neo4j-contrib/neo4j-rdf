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
import org.neo4j.rdf.store.testrepresentation.VerboseRepresentationStrategy;
import org.neo4j.triplestore.NeoTestCase;

public class TestRdfStore extends NeoTestCase
{
	public void testIt() throws Exception
	{
		RdfStore store = new RdfStoreImpl( neo(),
//			new DenseRepresentationStrategy( neo() ) );
			new VerboseRepresentationStrategy( neo() ) );
		Subject subject = new SubjectImpl( "http://henrik" );
		Subject otherSubject = new SubjectImpl( "http://emil" );
		Predicate namePredicate = new PredicateImpl( "http://name" );
		Predicate knowsPredicate = new PredicateImpl( "http://knows" );
		TripleObject object = new TripleObjectLiteral( "Henrik" );
		TripleObject otherObject = new TripleObjectLiteral( "Emil" );
		Statement statement = null;
		
		statement = new StatementImpl( subject, namePredicate, object );
		store.addStatement( statement );

		statement = new StatementImpl( otherSubject, namePredicate,
			otherObject );
		store.addStatement( statement );

		statement = new StatementImpl( subject, knowsPredicate,
			new TripleObjectResource( otherSubject ) );
		store.addStatement( statement );
	}
}
