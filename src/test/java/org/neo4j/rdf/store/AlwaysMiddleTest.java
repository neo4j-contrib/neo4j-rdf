package org.neo4j.rdf.store;

import java.util.List;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;

public class AlwaysMiddleTest extends StoreTestCase
{
    public void testSome() throws Exception
    {
        RdfStore store = new AlwaysMiddleStore( neo(), null );

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
//
//        debug( "S P ?O" );
//        Iterable<Statement> nicknames = store.getStatements(
//            new WildcardStatement( emil, NICKNAME,
//                new Wildcard( "nickname" ) ), true );
//        Set<String> emilsNicknames = new HashSet<String>(
//            Arrays.asList( nickEmil, nickEmpa ) );
//        for ( Statement statement : nicknames )
//        {
//            debug( statement.toString() );
//            assertTrue( emilsNicknames.remove( ( ( Literal )
//                statement.getObject() ).getValue() ) );
//        }
//        assertTrue( emilsNicknames.isEmpty() );
//
//        debug( "S ?P ?O" );
//        for ( Statement statement : store.getStatements( new WildcardStatement(
//            emil, new Wildcard( "predicate" ), new Wildcard( "value" ) ),
//                true ) )
//        {
//            debug( statement.toString() );
//        }
//
//        debug( "?S ?P L" );
//        for ( Statement statement : store.getStatements( new WildcardStatement(
//            new Wildcard( "subject" ), new Wildcard( "predicate" ),
//            new Literal( nickEmil ) ), true ) )
//        {
//            debug( statement.toString() );
//        }
//
//        debug( "?S ?P O" );
//        for ( Statement statement : store.getStatements( new WildcardStatement(
//            new Wildcard( "subject" ), new Wildcard( "predicate" ),
//            johan ), true ) )
//        {
//            debug( statement.toString() );
//        }
//
//        debug( "?S P L" );
//        for ( Statement statement : store.getStatements( new WildcardStatement(
//            new Wildcard( "subject" ), NICKNAME,
//            new Literal( nickEmil ) ), true ) )
//        {
//            debug( statement.toString() );
//        }
//
//        debug( "?S P O" );
//        for ( Statement statement : store.getStatements( new WildcardStatement(
//            new Wildcard( "subject" ), KNOWS, johan, new Context( "kdfjkdfj" ) ),
//            true ) )
//        {
//            debug( statement.toString() );
//        }

        removeStatements( store, statements, 1 );
        deleteEntireNodeSpace();
    }
}
