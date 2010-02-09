package org.neo4j.rdf.fulltext;

import java.io.File;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.Neo4jTestCase;

public class TestFulltextIndex extends Neo4jTestCase
{
    public void testSelfRepair() throws Exception
    {
        FulltextIndex index = new SimpleFulltextIndex( graphDb(),
            new File( "target/var/fulltext" ) );
        index.clear();
        Node node;
        
        Transaction tx = graphDb().beginTx();
        try
        {
            node = graphDb().createNode();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        
        tx = graphDb().beginTx();
        try
        {
            index.index( node, new Uri( "uri" ), "Mattias Persson" );
            index.index( node, new Uri( "uri" ), "Mattias Persson" );
            tx.success();
        }
        finally
        {
            index.end( true );
            tx.finish();
        }
        
        while ( !index.queueIsEmpty() )
        {
            Thread.sleep( 100 );
        }
        
        tx = graphDb().beginTx();
        try
        {
            int count = 0;
            for ( RawQueryResult result : index.search( "Mattias" ) )
            {
                assertEquals( node, result.getNode() );
                count++;
            }
            assertEquals( 1, count );
            tx.success();
        }
        finally
        {
            index.end( true );
            tx.finish();
        }
        
        while ( !index.queueIsEmpty() )
        {
            Thread.sleep( 100 );
        }
        
        index.clear();
        index.shutDown();
        
        tx = graphDb().beginTx();
        try
        {
            node.delete();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
}
