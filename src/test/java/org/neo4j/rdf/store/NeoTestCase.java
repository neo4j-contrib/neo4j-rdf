package org.neo4j.rdf.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.util.EntireGraphDeletor;
import org.neo4j.util.NeoUtil;

/**
 * Base class for the meta model tests.
 */
public abstract class NeoTestCase extends TestCase
{
	private static File basePath = new File( "target/var" );
    private static GraphDatabaseService neo;
    private static NeoUtil neoUtil;

    private Transaction tx;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        if ( neo == null )
        {
            File neoPath = new File( basePath, "neo" );
            deleteFileOrDirectory( neoPath );
            neo = new EmbeddedGraphDatabase( neoPath.getAbsolutePath() );
            Runtime.getRuntime().addShutdownHook( new Thread()
            {
                @Override
                public void run()
                {
                    neo.shutdown();
                }
            } );
            neoUtil = new NeoUtil( neo );
        }
        tx = neo().beginTx();
    }
    
    protected File getBasePath()
    {
        return basePath;
    }
    
    protected void deleteFileOrDirectory( File file )
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
        tx = neo.beginTx();
    }

    @Override
    protected void tearDown() throws Exception
    {
        tx.success();
        tx.finish();
        super.tearDown();
    }

    protected GraphDatabaseService neo()
    {
        return neo;
    }
    
    protected NeoUtil neoUtil()
    {
        return neoUtil;
    }

    protected void deleteEntireNodeSpace()
    {
        for ( Relationship rel : neo().getReferenceNode().getRelationships() )
        {
            Node node = rel.getOtherNode( neo().getReferenceNode() );
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
