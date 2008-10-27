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
import org.neo4j.util.index.IndexService;
import org.neo4j.util.index.NeoIndexService;

public abstract class StoreTestCase extends NeoWithIndexTestCase
{
    public static final Uri PERSON = new Uri( "http://person" );
    public static final Uri NAME = new Uri( "http://name" );
    public static final Uri NICKNAME = new Uri( "http://nickname" );
    public static final Uri KNOWS = new Uri( "http://knows" );
    public static final Context TEST_CONTEXT = new Context( "aTest" );

    @Override
    protected IndexService instantiateIndexService()
    {
        return new NeoIndexService( neo() );
    }

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

    protected void remove( RdfStore store, WildcardStatement statement,
        int numberOfTimes )
    {
        while ( numberOfTimes-- > 0 )
        {
            debug( "removeStatement " + statement );
            store.removeStatements( statement );
        }
    }

    protected Statement statement( String subject, String predicate,
        Resource object, Context context )
    {
        return new CompleteStatement( new Uri( subject ), new Uri( predicate ),
            object, context, null );
    }

    protected Statement statement( String subject, String predicate,
        Object object, Context context )
    {
        return new CompleteStatement( new Uri( subject ), new Uri( predicate ),
            new Literal( object ), context, null );
    }

    protected void removeStatements( RdfStore store,
        List<CompleteStatement> statements )
    {
        removeStatements( store, statements, 1 );
    }

    protected void removeStatements( RdfStore store,
        List<CompleteStatement> statements, int numberOfTimesForEach )
    {
        while ( !statements.isEmpty() )
        {
            CompleteStatement statement = statements.remove(
                new Random().nextInt( statements.size() ) );
            WildcardStatement wildcardStatement =
                statement.asWildcardStatement();
            remove( store, wildcardStatement, numberOfTimesForEach );
        }
    }

    protected List<CompleteStatement> addStatements(
        RdfStore store, CompleteStatement... statements )
    {
        ArrayList<CompleteStatement> list = new ArrayList<CompleteStatement>();
        for ( CompleteStatement statement : statements )
        {
            add( store, statement, 1 );
            list.add( statement );
        }
        return list;
    }
}
