package org.neo4j.rdf.store.representation.standard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.neo4j.api.core.Node;
import org.neo4j.util.index.IndexHits;
import org.neo4j.util.index.IndexService;
import org.neo4j.util.index.Isolation;

public class FilteringIndexService implements IndexService
{
    private IndexService indexService;
    private Collection<String> allowedKeys = new HashSet<String>();
    
    public FilteringIndexService( IndexService indexService,
        Collection<String> allowedKeys )
    {
        this.indexService = indexService;
        this.allowedKeys.addAll( allowedKeys );
    }
    
    public FilteringIndexService( IndexService indexService,
        File fileWithKeys )
    {
        BufferedReader in = null;
        try
        {
            in = new BufferedReader( new FileReader( fileWithKeys ) );
            String line = null;
            while ( ( line = in.readLine() ) != null )
            {
                allowedKeys.add( line );
            }
        }
        catch ( IOException e )
        {
            // Boring
        }
        finally
        {
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( IOException e )
                {
                    // Boring.
                }
            }
        }
    }
    
    private boolean shouldIndexKey( String key )
    {
        return this.allowedKeys.contains( key );
    }
    
    public Collection<String> getAllowedKeys()
    {
        return this.allowedKeys;
    }

    public IndexHits getNodes( String key, Object value )
    {
        return this.indexService.getNodes( key, value );
    }

    public Node getSingleNode( String key, Object value )
    {
        return this.indexService.getSingleNode( key, value );
    }

    public void index( Node node, String key, Object value )
    {
        if ( !shouldIndexKey( key ) )
        {
            return;
        }
        this.indexService.index( node, key, value );
    }

    public void removeIndex( Node node, String key, Object value )
    {
        if ( !shouldIndexKey( key ) )
        {
            return;
        }
        this.indexService.removeIndex( node, key, value );
    }

    public void setIsolation( Isolation level )
    {
        this.indexService.setIsolation( level );
    }

    public void shutdown()
    {
        this.indexService.shutdown();
    }
}
