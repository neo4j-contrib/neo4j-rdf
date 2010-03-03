package org.neo4j.rdf.store;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import junit.framework.TestCase;

public class TestInlineShutdown extends TestCase
{
    public void testInlineShutdown() throws Exception
    {
        GraphDatabaseService graphDb = new EmbeddedGraphDatabase(
                new File( Neo4jTestCase.getBasePath(), "shutdown" ).getAbsolutePath() );
        IndexService index = new LuceneIndexService( graphDb );
        VerboseQuadStore store = new VerboseQuadStore( graphDb, index );
        store.setShutdownNeo4jInstancesUponShutdown( true );
        store.shutDown();
        
        try
        {
            Transaction tx = graphDb.beginTx();
            try
            {
                graphDb.createNode();
                tx.success();
            }
            finally
            {
                tx.finish();
            }
            fail( "Shouldn't be able to create a node in graph db which is shut down" );
        }
        catch ( Exception e )
        {
            // OK
        }
    }

    public void testNormalShutdown() throws Exception
    {
        GraphDatabaseService graphDb = new EmbeddedGraphDatabase(
                new File( Neo4jTestCase.getBasePath(), "shutdown" ).getAbsolutePath() );
        IndexService index = new LuceneIndexService( graphDb );
        VerboseQuadStore store = new VerboseQuadStore( graphDb, index );
        store.shutDown();
        
        Transaction tx = graphDb.beginTx();
        try
        {
            graphDb.createNode();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
}
