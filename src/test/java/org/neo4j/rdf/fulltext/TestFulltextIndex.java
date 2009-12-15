package org.neo4j.rdf.fulltext;

import java.io.File;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.NeoTestCase;

public class TestFulltextIndex extends NeoTestCase
{
    public void testSelfRepair() throws Exception
    {
        FulltextIndex index = new SimpleFulltextIndex( neo(),
            new File( "target/var/fulltext" ) );
        index.clear();
        Node node;
        
        Transaction tx = neo().beginTx();
        try
        {
            node = neo().createNode();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        
        tx = neo().beginTx();
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
        
        tx = neo().beginTx();
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
        
        tx = neo().beginTx();
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
