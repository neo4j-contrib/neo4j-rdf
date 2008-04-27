package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.WildcardStatement;

public abstract class StoreTestCase extends NeoTestCase
{
    public static final Uri PERSON = new Uri( "http://person" );
    public static final Uri NAME = new Uri( "http://name" );
    public static final Uri NICKNAME = new Uri( "http://nickname" );
    public static final Uri KNOWS = new Uri( "http://knows" );
    public static final Context TEST_CONTEXT = new Context( "aTest" );

    protected void debug( String text )
    {
        System.out.println( text );
    }

    protected void add( RdfStore store, Statement statement, int numberOfTimes )
    {
        if ( !(statement instanceof CompleteStatement ) )
        {
            throw new IllegalArgumentException(
                "Can only add complete statements " );
        }
        while ( numberOfTimes-- > 0 )
        {
            debug( "addStatement " + statement );
            store.addStatements( ( CompleteStatement ) statement );
        }
    }

    protected void addTwice( RdfStore store, Statement statement )
    {
        add( store, statement, 2 );
    }

    protected void remove( RdfStore store, WildcardStatement statement,
        int numberOfTimes )
    {
        while ( numberOfTimes-- > 0 )
        {
            debug( "removeStatement " + statement );
            store.removeStatements( statement );
        }
    }

    protected void removeTwice( RdfStore store, WildcardStatement statement )
    {
        remove( store, statement, 2 );
    }

    protected Statement statement( String subject, String predicate,
        Resource object, Context context, Context... contexts )
    {
        return new CompleteStatement( new Uri( subject ), new Uri( predicate ),
            object, context, contexts );
    }

    protected Statement statement( String subject, String predicate,
        Object object, Context context, Context... contexts )
    {
        return new CompleteStatement( new Uri( subject ), new Uri( predicate ),
            new Literal( object ), context, contexts );
    }

    protected void removeStatements( RdfStore store,
        List<Statement> statements )
    {
        removeStatements( store, statements, 2 );
    }

    protected void removeStatements( RdfStore store,
        List<Statement> statements, int numberOfTimesForEach )
    {
        while ( !statements.isEmpty() )
        {
            Statement statement = statements.remove(
                new Random().nextInt( statements.size() ) );
            WildcardStatement wildcardStatement =
                statement instanceof CompleteStatement ?
                    ( ( CompleteStatement ) statement ).asWildcardStatement() :
                        ( WildcardStatement ) statement;
            remove( store, wildcardStatement, numberOfTimesForEach );
        }
    }

    protected List<Statement> addStatements(
        RdfStore store, Statement... statements )
    {
        ArrayList<Statement> list = new ArrayList<Statement>();
        for ( Statement statement : statements )
        {
            add( store, ( CompleteStatement ) statement, 1 );
            list.add( statement );
        }
        return list;
    }
}
