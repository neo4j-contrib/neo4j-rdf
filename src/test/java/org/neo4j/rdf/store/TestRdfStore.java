package org.neo4j.rdf.store;

/**
 * Tests an {@link RdfStore}.
 */
public class TestRdfStore extends StoreTestCase
{
    public void testNothing()
    {
    }

//    /**
//     * Tests an {@link RdfStore} with a {@link DenseRepresentationStrategy}.
//     * @throws Exception if there's an error in the test.
//     */
//	public void testDense() throws Exception
//	{
//	    RepresentationExecutor executor = new UriBasedExecutor( neo(),
//	        indexService(), null );
//		RdfStore store = new RdfStoreImpl( neo(),
//			new DenseRepresentationStrategy( executor, null ) );
//		applyAndRemoveStatements( store );
//		deleteEntireNodeSpace();
//	}
//
//    /**
//     * Tests an {@link RdfStore} with a {@link VerboseRepresentationStrategy}.
//     * @throws Exception if there's an error in the test.
//     */
//	public void testVerbose() throws Exception
//	{
//        RepresentationExecutor executor = new UriBasedExecutor( neo(),
//            indexService(), null );
//		RdfStore store = new RdfStoreImpl( neo(),
//			new VerboseRepresentationStrategy( executor, null ) );
//		applyAndRemoveStatements( store );
//		deleteEntireNodeSpace();
//	}
//
//    /**
//     * Tests an {@link RdfStore} with a {@link VerboseRepresentationStrategy}
//     * with a {@link MetaStructure}.
//     * @throws Exception if there's an error in the test.
//     */
//	public void testVerboseMeta() throws Exception
//	{
//	    MetaStructure meta = new MetaStructureImpl( neo() );
//	    RepresentationExecutor executor = new UriBasedExecutor( neo(),
//	        indexService(), meta );
//	    RepresentationStrategy strategy = new VerboseRepresentationStrategy(
//	        executor, meta );
//	    RdfStore store = new RdfStoreImpl( neo(), strategy );
//	    MetaStructureClass personClass =
//	        meta.getGlobalNamespace().getMetaClass( PERSON.getUriAsString(),
//	            true );
//        meta.getGlobalNamespace().getMetaProperty( NAME.getUriAsString(),
//            true );
//        MetaStructureProperty knowsProperty =
//            meta.getGlobalNamespace().getMetaProperty( KNOWS.getUriAsString(),
//                true );
//	    List<Statement> statements = applyStatements( store );
//
//	    // Verify
//	    Node knowsPropertyNode = knowsProperty.node();
//	    assertEquals( 3, personClass.getInstances().size() );
//	    assertEquals( 2, countIterable( knowsPropertyNode.getRelationships(
//	        VerboseRepresentationStrategy.RelTypes.
//	            CONNECTOR_HAS_PREDICATE ) ) );
//
//	    removeStatements( store, statements );
//	    deleteEntireNodeSpace();
//	}
//
//	private List<Statement> applyStatements( RdfStore store )
//	{
//	    String typePredicate = AbstractUriBasedExecutor.RDF_TYPE_URI;
//	    Uri personClass = new Uri( PERSON.getUriAsString() );
//		String subject = "http://henrik";
//		String otherSubject = "http://emil";
//		String thirdSubject = "http://mattias";
//		String namePredicate = NAME.getUriAsString();
//		String knowsPredicate = KNOWS.getUriAsString();
//		Object object = "Henrik";
//		Object otherObject = "Emil";
//		Object thirdObject = "Mattias";
//
//		Statement subjectTypeStatement =
//		    statement( subject, typePredicate, personClass );
//		addTwice( store, subjectTypeStatement );
//		Statement subjectNameStatement =
//			statement( subject, namePredicate, object );
//		addTwice( store, subjectNameStatement );
//
//        Statement otherSubjectTypeStatement =
//            statement( otherSubject, typePredicate, personClass );
//        addTwice( store, otherSubjectTypeStatement );
//		Statement otherSubjectNameStatement =
//		    statement( otherSubject, namePredicate, otherObject );
//		addTwice( store, otherSubjectNameStatement );
//
//		Statement thirdSubjectNameStatement =
//		    statement( thirdSubject, namePredicate, thirdObject );
//		addTwice( store, thirdSubjectNameStatement );
//        Statement thirdSubjectTypeStatement =
//            statement( thirdSubject, typePredicate, personClass );
//        addTwice( store, thirdSubjectTypeStatement );
//
//		Statement knowsStatement =
//		    statement( subject, knowsPredicate, new Uri( otherSubject ) );
//		addTwice( store, knowsStatement );
//		Statement otherKnowsStatement =
//		    statement( subject, knowsPredicate, new Uri( thirdSubject ) );
//		addTwice( store, otherKnowsStatement );
//
//		return new ArrayList<Statement>(
//		    Arrays.asList(
//		        subjectTypeStatement,
//		        subjectNameStatement,
//		        otherSubjectTypeStatement,
//		        otherSubjectNameStatement,
//		        thirdSubjectTypeStatement,
//		        thirdSubjectNameStatement,
//		        knowsStatement,
//		        otherKnowsStatement ) );
//	}
//
//	private void applyAndRemoveStatements( RdfStore store )
//	{
//	    removeStatements( store, applyStatements( store ) );
//	}
}
