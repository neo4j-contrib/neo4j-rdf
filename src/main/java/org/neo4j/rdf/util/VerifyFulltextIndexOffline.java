package org.neo4j.rdf.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.fulltext.SimpleFulltextIndex;
import org.neo4j.rdf.store.CachingLuceneIndexService;
import org.neo4j.rdf.store.RdfStore;
import org.neo4j.rdf.store.VerboseQuadStore;
import org.neo4j.index.IndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class VerifyFulltextIndexOffline
{
    public static void main( String[] args ) throws IOException
    {
        final GraphDatabaseService graphDb = new EmbeddedGraphDatabase( args[ 0 ] );
        final IndexService indexService =
            new CachingLuceneIndexService( graphDb );
        final FulltextIndex fulltextIndex = new SimpleFulltextIndex( graphDb,
            new File( args[ 1 ] ) );
        final RdfStore store = new VerboseQuadStore( graphDb, indexService,
            null, fulltextIndex );
        
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                System.out.println( "Shutting down the store and Neo4j n' all" );
                store.shutDown();
                indexService.shutdown();
                graphDb.shutdown();
                System.out.println( "Everything shut down nicely" );
            }
        } );
        
        System.out.print( "Enter query or just ENTER for the entire index: " );
        String query = new BufferedReader(
            new InputStreamReader( System.in ) ).readLine();
        query = query == null || query.trim().length() == 0 ? null : query;
        
        System.out.println( "Running verification for " +
            ( query == null ? "the entire index" : "query '" + query + "'" ) );
        System.out.println( "You can tail -f the file to see progress, " +
            "it's called something like verify-fulltextindex-123...." );
        boolean everythingIsOk = store.verifyFulltextIndex( query );
        if ( everythingIsOk )
        {
            System.out.println( "Index is OK" );
        }
        System.out.println( "Verification done, " +
            "see file for detailed results" );
        
        System.exit( 0 );
    }
}
