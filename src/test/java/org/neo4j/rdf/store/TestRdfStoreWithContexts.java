package org.neo4j.rdf.store;

import java.util.List;

import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureImpl;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.RepresentationExecutor;
import org.neo4j.rdf.store.representation.RepresentationStrategy;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.UriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseRepresentationStrategy;

public class TestRdfStoreWithContexts extends StoreTestCase
{
    public void testWithContexts()
    {
        MetaStructure meta = new MetaStructureImpl( neo() );
        RepresentationExecutor executor = new UriBasedExecutor( neo(),
            AbstractUriBasedExecutor.newIndex( neo() ), meta );
        RepresentationStrategy strategy = new VerboseRepresentationStrategy(
            executor, meta );
        RdfStore store = new RdfStoreImpl( neo(), strategy );

        Context c1 = new Context( "http://ns1" );
        Context c2 = new Context( "http://ns2" );
        meta.getGlobalNamespace().getMetaClass( PERSON_CLASS, true );
        meta.getGlobalNamespace().getMetaProperty( NAME_PROPERTY, true );
        meta.getGlobalNamespace().getMetaProperty( NICKNAME_PROPERTY, true );
        meta.getGlobalNamespace().getMetaProperty( KNOWS_PROPERTY, true );

        String subject = "http://mattias";
        String otherSubject = "http://emil";
        String name = "Mattias";
        String nickname1 = "Matte";
        String nickname2 = "Mathew";
        Statement sNameC1 = statement( subject, NAME_PROPERTY, name, c1 );
        Statement sNameC2 = statement( subject, NAME_PROPERTY, name, c2 );
        Statement sNick1C1 = statement( subject, NICKNAME_PROPERTY,
            nickname1, c1 );
        Statement sNick1C2 = statement( subject, NICKNAME_PROPERTY,
            nickname1, c2 );
        Statement sNick2C2 = statement( subject, NICKNAME_PROPERTY,
            nickname2, c2 );
        Statement sPerson = statement( subject,
            AbstractUriBasedExecutor.RDF_TYPE_URI, new Uri( PERSON_CLASS ) );
        Statement sKnowsC1 = statement( subject, KNOWS_PROPERTY,
            new Uri( otherSubject ), c1 );
        Statement sKnowsC2 = statement( subject, KNOWS_PROPERTY,
            new Uri( otherSubject ), c2 );
        List<Statement> statements = addStatements( store,
            sNameC1
            ,
            sNameC2
            ,
            sNick1C1
            ,
            sNick1C2
            ,
            sNick2C2
            ,
            sPerson
            ,
            sKnowsC1
            ,
            sKnowsC2
            );
        removeStatements( store, statements, 1 );
//        Node node = neo().getNodeById( 9 );
//        NeoUtil neoUtil = new NeoUtil( neo() );
//        for ( String key : node.getPropertyKeys() )
//        {
//            System.out.println( key + "=" + neoUtil.neoPropertyAsList(
//                node.getProperty( key ) ) );
//        }
        deleteEntireNodeSpace();
    }
}
