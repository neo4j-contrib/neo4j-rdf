package org.neo4j.rdf.store;

import java.util.List;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.RepresentationExecutor;
import org.neo4j.rdf.store.representation.RepresentationStrategy;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.AlwaysMiddleExecutor;
import org.neo4j.rdf.store.representation.standard.AlwaysMiddleNodesStrategy;

public class AlwaysMiddleTest extends StoreTestCase
{
    public void testSome() throws Exception
    {
        RepresentationExecutor executor = new AlwaysMiddleExecutor(
            neo(), AbstractUriBasedExecutor.newIndex( neo() ), null );
        RepresentationStrategy strategy = new AlwaysMiddleNodesStrategy(
            executor, null );
        RdfStore store = new RdfStoreImpl( neo(), strategy );

        Uri emil = new Uri( "http://emil" );
        Uri johan = new Uri( "http://johan" );
        String nickEmil = "Emil";
        String nickEmpa = "Empa";
        Context c1 = new Context( "http://c1" );
        Context c2 = new Context( "http://c2" );

        CompleteStatement emilIsPerson = new CompleteStatement( emil,
            new Uri( AbstractUriBasedExecutor.RDF_TYPE_URI ), PERSON );
        CompleteStatement johanIsPerson = new CompleteStatement( johan,
            new Uri( AbstractUriBasedExecutor.RDF_TYPE_URI ), PERSON );

        CompleteStatement emilKnowsJohanC1 = new CompleteStatement( emil,
            KNOWS, johan, c1 );
        CompleteStatement emilKnowsJohanC2 = new CompleteStatement( emil,
            KNOWS, johan, c2 );
        CompleteStatement emilNickEmilC1 = new CompleteStatement( emil,
            NICKNAME, new Literal( nickEmil ), c1 );
        CompleteStatement emilNickEmpaC2 = new CompleteStatement( emil,
            NICKNAME, new Literal( nickEmpa ), c2 );
        CompleteStatement emilNickEmilC2 = new CompleteStatement( emil,
            NICKNAME, new Literal( nickEmil ), c2 );
        List<Statement> statements = this.addStatements( store,
            emilIsPerson,
            johanIsPerson,
            emilKnowsJohanC1,
            emilKnowsJohanC2,
            emilNickEmilC1,
            emilNickEmpaC2,
            emilNickEmilC2
            );
        removeStatements( store, statements, 1 );
        deleteEntireNodeSpace();
    }
}
