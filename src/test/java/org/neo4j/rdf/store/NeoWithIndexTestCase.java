package org.neo4j.rdf.store;

import org.neo4j.util.index.IndexService;

public abstract class NeoWithIndexTestCase extends NeoTestCase
{
    private IndexService indexService;
    
    private void createIndexServiceIfNeeded()
    {
        if ( indexService() == null )
        {
            setIndexService( instantiateIndexService() );
        }
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
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        createIndexServiceIfNeeded();
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        if ( indexService() != null )
        {
            indexService().shutdown();
            setIndexService( null );
        }
        super.tearDown();
    }
}
