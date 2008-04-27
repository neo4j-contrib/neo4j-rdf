package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Transaction;
import org.neo4j.util.EntireGraphDeletor;
import org.neo4j.util.index.IndexService;

/**
 * Base class for the meta model tests.
 */
public abstract class NeoTestCase extends TestCase
{
    private static NeoService neo;
    private IndexService indexService = null;

    private Transaction tx;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();        
        if ( neo == null )
        {
            neo = new EmbeddedNeo( "var/test/neo" );
            Runtime.getRuntime().addShutdownHook( new Thread()
            {
                @Override
                public void run()
                {
                    neo.shutdown();
                }
            } );
        }
        tx = neo().beginTx();
        createIndexServiceIfNeeded();
    }

    private void createIndexServiceIfNeeded()
    {
        if ( indexService() == null )
        {
            setIndexService( instantiateIndexService() );
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        if ( indexService() != null )
        {
            indexService().shutdown();
            setIndexService( null );
        }
        tx.success();
        tx.finish();
        super.tearDown();
    }
    
    /**
     * In every setUp(), this class will check if there's an existing
     * IndexSerivce (using {@link #indexService()}). If not, one will be
     * created by invoking this method.
     * @return the newly instantiated index service
     */
    protected IndexService instantiateIndexService()
    {
        return null;
    }
    
    private void setIndexService( IndexService indexService )
    {
        this.indexService = indexService;
    }
    
    protected IndexService indexService()
    {
        return this.indexService;
    }

    protected NeoService neo()
    {
        return neo;
    }

    protected void deleteEntireNodeSpace()
    {
//        tx.success();
//        tx.finish();
//        try
//        {
//            Thread.sleep( 500 );
//        }
//        catch ( InterruptedException e )
//        {
//        }
//        tx = neo.beginTx();
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
