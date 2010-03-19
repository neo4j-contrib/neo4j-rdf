package org.neo4j.rdf.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.util.EntireGraphDeletor;
import org.neo4j.util.GraphDatabaseUtil;

public abstract class Neo4jTestCase
{
	private static File basePath = new File( "target/var" );
    private static GraphDatabaseService graphDb;
    private static GraphDatabaseUtil graphDbUtil;
    private static IndexService index;

    private Transaction tx;

    @BeforeClass
    public static void setUpDb() throws Exception
    {
        File path = new File( basePath, "neo4j" );
        deleteFileOrDirectory( path );
        graphDb = new EmbeddedGraphDatabase( path.getAbsolutePath() );
        graphDbUtil = new GraphDatabaseUtil( graphDb );
        index = new LuceneIndexService( graphDb );
    }
    
    @Before
    public void setUpTest()
    {
        tx = graphDb().beginTx();
    }
    
    @After
    public void tearDownTest()
    {
        tx.success();
        tx.finish();
    }
    
    @AfterClass
    public static void shutdownDb()
    {
        index.shutdown();
        graphDb.shutdown();
    }

    public static File getBasePath()
    {
        return basePath;
    }
    
    protected static void deleteFileOrDirectory( File file )
    {
        if ( !file.exists() )
        {
            return;
        }
        
        if ( file.isDirectory() )
        {
            for ( File child : file.listFiles() )
            {
                deleteFileOrDirectory( child );
            }
        }
        else
        {
            file.delete();
        }
    }

    protected void restartTx()
    {
        tx.success();
        tx.finish();
        tx = graphDb.beginTx();
    }

    protected static GraphDatabaseService graphDb()
    {
        return graphDb;
    }
    
    protected static GraphDatabaseUtil graphDbUtil()
    {
        return graphDbUtil;
    }
    
    protected static IndexService indexService()
    {
        return index;
    }

    protected void deleteEntireNodeSpace()
    {
        for ( Relationship rel : graphDb().getReferenceNode().getRelationships() )
        {
            Node node = rel.getOtherNode( graphDb().getReferenceNode() );
            rel.delete();
            new EntireGraphDeletor().delete( node );
        }
    }
    
    protected <T> void assertCollection( Collection<T> collection, T... items )
    {
        String collectionString = join( ", ", collection.toArray() );
        assertEquals( collectionString, items.length, collection.size() );
        for ( T item : items )
        {
            assertTrue( collection.contains( item ) );
        }
    }

    protected <T> Collection<T> asCollection( Iterable<T> iterable )
    {
        List<T> list = new ArrayList<T>();
        for ( T item : iterable )
        {
            list.add( item );
        }
        return list;
    }

    protected <T> String join( String delimiter, T... items )
    {
        StringBuffer buffer = new StringBuffer();
        for ( T item : items )
        {
            if ( buffer.length() > 0 )
            {
                buffer.append( delimiter );
            }
            buffer.append( item.toString() );
        }
        return buffer.toString();
    }

    protected <T> int countIterable( Iterable<T> iterable )
    {
        int counter = 0;
        Iterator<T> itr = iterable.iterator();
        while ( itr.hasNext() )
        {
            itr.next();
            counter++;
        }
        return counter;
    }
}
