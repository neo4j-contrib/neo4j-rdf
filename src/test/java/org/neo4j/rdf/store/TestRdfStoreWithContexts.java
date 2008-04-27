package org.neo4j.rdf.store;


public class TestRdfStoreWithContexts extends StoreTestCase
{
//    public void testWithContexts()
//    {
//        MetaStructure meta = new MetaStructureImpl( neo() );
//        RepresentationExecutor executor = new UriBasedExecutor( neo(),
//            AbstractUriBasedExecutor.newIndex( neo() ), meta );
//        RepresentationStrategy strategy = new VerboseRepresentationStrategy(
//            executor, meta );
//        RdfStore store = new RdfStoreImpl( neo(), strategy );
//
//        Context c1 = new Context( "http://ns1" );
//        Context c2 = new Context( "http://ns2" );
//        meta.getGlobalNamespace().getMetaClass( PERSON.getUriAsString(),
//            true );
//        meta.getGlobalNamespace().getMetaProperty( NAME.getUriAsString(),
//            true );
//        meta.getGlobalNamespace().getMetaProperty( NICKNAME.getUriAsString(),
//            true );
//        meta.getGlobalNamespace().getMetaProperty( KNOWS.getUriAsString(),
//            true );
//
//        String subject = "http://mattias";
//        String otherSubject = "http://emil";
//        String name = "Mattias";
//        String nickname1 = "Matte";
//        String nickname2 = "Mathew";
//        Statement sNameC1 = statement( subject, NAME.getUriAsString(),
//            name, c1 );
//        Statement sNameC2 = statement( subject, NAME.getUriAsString(),
//            name, c2 );
//        Statement sNick1C1 = statement( subject, NICKNAME.getUriAsString(),
//            nickname1, c1 );
//        Statement sNick1C2 = statement( subject, NICKNAME.getUriAsString(),
//            nickname1, c2 );
//        Statement sNick2C2 = statement( subject, NICKNAME.getUriAsString(),
//            nickname2, c2 );
//        Statement sPerson = statement( subject,
//            AbstractUriBasedExecutor.RDF_TYPE_URI, PERSON );
//        Statement sKnowsC1 = statement( subject, KNOWS.getUriAsString(),
//            new Uri( otherSubject ), c1 );
//        Statement sKnowsC2 = statement( subject, KNOWS.getUriAsString(),
//            new Uri( otherSubject ), c2 );
//        List<Statement> statements = addStatements( store,
//            sNameC1
//            ,
//            sNameC2
//            ,
//            sNick1C1
//            ,
//            sNick1C2
//            ,
//            sNick2C2
//            ,
//            sPerson
//            ,
//            sKnowsC1
//            ,
//            sKnowsC2
//            );
//        removeStatements( store, statements, 1 );
////        Node node = neo().getNodeById( 9 );
////        NeoUtil neoUtil = new NeoUtil( neo() );
////        for ( String key : node.getPropertyKeys() )
////        {
////            System.out.println( key + "=" + neoUtil.neoPropertyAsList(
////                node.getProperty( key ) ) );
////        }
//        deleteEntireNodeSpace();
//    }
}
