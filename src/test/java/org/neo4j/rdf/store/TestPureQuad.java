package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.neo4j.neometa.structure.DatatypeClassRange;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureClassRange;
import org.neo4j.neometa.structure.MetaStructureImpl;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.standard.MetaEnabledUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.PureQuadRepresentationStrategy;

public class TestPureQuad extends StoreTestCase
{
    private static final Uri PERSON = new Uri( "http://person" );
    private static final Uri NICKNAME = new Uri( "http://nickname" );
    private static final Uri KNOWS = new Uri( "http://knows" );

    private static final Random RANDOM = new Random();

    private MetaStructure newMetaStructure()
    {
        MetaStructure meta = new MetaStructureImpl( neo() );
        MetaStructureClass personClass = meta.getGlobalNamespace().
            getMetaClass( PERSON.getUriAsString(), true );
        MetaStructureProperty nameProperty = meta.getGlobalNamespace().
            getMetaProperty( NICKNAME.getUriAsString(), true );
        MetaStructureProperty knowsProperty = meta.getGlobalNamespace().
            getMetaProperty( KNOWS.getUriAsString(), true );
        personClass.getDirectProperties().add( nameProperty );
        personClass.getDirectProperties().add( knowsProperty );
        nameProperty.setRange( new DatatypeClassRange( String.class ) );
        knowsProperty.setRange( new MetaStructureClassRange( personClass ) );
        return meta;
    }

    private RdfStore newRdfStore()
    {
        MetaStructure meta = newMetaStructure();
        return new PureQuadRdfStore( neo(), meta,
            new PureQuadRepresentationStrategy( neo(), meta ) );
    }

    public void testQuad() throws Exception
    {
        RdfStore store = newRdfStore();
        Uri emil = new Uri( "http://emil" );
        Uri johan = new Uri( "http://johan" );
        String nickEmil = "Emil";
        String nickEmpa = "Empa";
        Context c1 = new Context( "http://c1" );
        Context c2 = new Context( "http://c2" );

        CompleteStatement emilIsPerson = new CompleteStatement( emil,
            new Uri( MetaEnabledUriBasedExecutor.RDF_TYPE_URI ), PERSON );
        CompleteStatement johanIsPerson = new CompleteStatement( johan,
            new Uri( MetaEnabledUriBasedExecutor.RDF_TYPE_URI ), PERSON );

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
            emilNickEmilC2 );

        Iterable<Statement> nicknames = store.getStatements(
            new WildcardStatement( emil, NICKNAME,
                new Wildcard( "nickname" ) ), true );
        Set<String> emilsNicknames = new HashSet<String>(
            Arrays.asList( nickEmil, nickEmpa ) );
        for ( Statement statement : nicknames )
        {
            System.out.println( statement );
            assertTrue( emilsNicknames.remove( ( ( Literal )
                statement.getObject() ).getValue() ) );
        }
        assertTrue( emilsNicknames.isEmpty() );

        removeStatements( store, statements, 1 );
        deleteEntireNodeSpace();
    }

    public void testHeavy() throws Exception
    {
        RdfStore store = newRdfStore();
        List<Uri> persons = new ArrayList<Uri>();
        for ( int i = 0; i < 1000; i++ )
        {
            Uri person = new Uri( PERSON.getUriAsString() +
                "_" + RANDOM.nextInt( 1000 ) );
            store.addStatements( new CompleteStatement( person, new Uri(
                MetaEnabledUriBasedExecutor.RDF_TYPE_URI ), PERSON ) );
            int count = RANDOM.nextInt( 10 );
            for ( int ii = 0; ii < count; ii++ )
            {
                store.addStatements( new CompleteStatement( person,
                    NICKNAME, new Literal( newRandomName() ) ) );
            }

            if ( persons.isEmpty() )
            {
                continue;
            }
            count = RANDOM.nextInt( Math.min( persons.size(),
                RANDOM.nextInt( 20 ) ) );
            for ( int ii = 0; ii < count; ii++ )
            {
                store.addStatements( new CompleteStatement( person, NICKNAME,
                    persons.get( RANDOM.nextInt( persons.size() ) ) ) );
            }
            persons.add( person );
        }
        deleteEntireNodeSpace();
    }

    private Object newRandomName()
    {
        String letters = "abcdefghijklmnopqrst";
        int length = RANDOM.nextInt( 10 ) + 2;
        StringBuffer buffer = new StringBuffer();
        for ( int i = 0; i < length; i++ )
        {
            buffer.append( letters.charAt(
                RANDOM.nextInt( letters.length() ) ) );
        }
        return buffer.toString();
    }
}
