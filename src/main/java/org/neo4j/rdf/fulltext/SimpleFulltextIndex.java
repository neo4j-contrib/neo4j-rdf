package org.neo4j.rdf.fulltext;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.transaction.SystemException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.NotFoundException;
import org.neo4j.rdf.fulltext.PersistentQueue.Entry;
import org.neo4j.rdf.fulltext.VerificationHook.Status;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.util.TemporaryLogger;
import org.neo4j.util.FilteringIterator;
import org.neo4j.util.IteratorAsIterable;
import org.neo4j.util.NeoUtil;
import org.neo4j.util.PrefetchingIterator;

/**
 * A {@link FulltextIndex} using lucene.
 * The query format (see the search method) is a plain lucene query, but with
 * the addition that an AND operator is squeezed in between every word making
 * it and AND search by default, instead of OR.
 * 
 * When you call the index and removeIndex methods a temporary log is created
 * and a call to the end method will write all those additions to the queue
 * to be indexed in the near future. The "txId" i.e. transaction id is really
 * just the javax.transaction.Transaction object's hashCode() value at the
 * moment. That is what you'll have to pass in to the
 * end( boolean commit, int txId ) method if you choose not to use the
 * end( boolean commit ) method which figures it out itself, provided that
 * you are in a transaction at the time of the call.
 */
public class SimpleFulltextIndex implements FulltextIndex
{
    /**
     * The literal node id
     */
    private static final String KEY_ID = "id";
    private static final String KEY_INDEX = "index";
    private static final String KEY_PREDICATE = "predicate";
    private static final String KEY_INDEX_SOURCE = "index_source";
    private static final String SNIPPET_DELIMITER = "...";
    private static final int BATCH_SIZE = 100;
    
    private LiteralReader literalReader = new SimpleLiteralReader();
    private String directoryPath;
    private String queuePath;
    private Directory directory;
    private Analyzer analyzer = new Analyzer()
    {
        @Override
        public TokenStream tokenStream( String fieldName, Reader reader )
        {
            return new LowerCaseFilter( new WhitespaceTokenizer( reader ) );
        }
    };
    private NeoService neo;
    private NeoUtil neoUtil;
    private Map<Integer, Collection<Object[]>> toIndex =
        Collections.synchronizedMap(
            new HashMap<Integer, Collection<Object[]>>() );
    private PersistentQueue indexingQueue;
    private IndexingThread indexingThread;
    private Formatter highlightFormatter;
    private Set<String> predicateFilter;
    private IndexSearcher indexSearcher;
    
    public SimpleFulltextIndex( NeoService neo, File storagePath )
    {
        this( neo, storagePath, null );
    }
    
    public SimpleFulltextIndex( NeoService neo, File storagePath,
        Collection<String> predicateFilter )
    {
        this( neo, storagePath, null, null, predicateFilter );
    }
    
    public SimpleFulltextIndex( NeoService neo, File storagePath,
        String highlightPreTag, String highlightPostTag,
        Collection<String> predicateFilter )
    {
        if ( highlightPreTag == null || highlightPostTag == null )
        {
            this.highlightFormatter = new SimpleHTMLFormatter();
        }
        else
        {
            this.highlightFormatter = new SimpleHTMLFormatter(
                highlightPreTag, highlightPostTag );
        }
        
        this.predicateFilter = predicateFilter == null ? null :
            new HashSet<String>( predicateFilter );
        this.directoryPath = storagePath.getAbsolutePath();
        this.queuePath = this.directoryPath + "-queue";
        this.neo = neo;
        this.neoUtil = new NeoUtil( neo );
        startUpDirectoryAndThread();
    }
    
    private void startUpDirectoryAndThread()
    {
        this.indexingQueue = new PersistentQueue( new File( queuePath ) );
        this.indexingQueue.setAutoCompleteEntries( false );
        try
        {
            cleanWriteLocks( new File( directoryPath ) );
            createLuceneDirectory();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        
        this.indexingThread = new IndexingThread();
        this.indexingThread.start();
    }
    
    private void cleanWriteLocks( File path )
    {
        if ( !path.isDirectory() )
        {
            return;
        }
        for ( File file : path.listFiles() )
        {
            if ( file.isDirectory() )
            {
                cleanWriteLocks( file );
            }
            else if ( file.getName().equals( "write.lock" ) )
            {
                boolean success = file.delete();
                assert success;
            }
        }
    }
    
    public void clear()
    {
        internalShutDown();
        delete();
        startUpDirectoryAndThread();
    }
    
    private void createLuceneDirectory() throws IOException
    {
        if ( !IndexReader.indexExists( directoryPath ) )
        {
            new File( directoryPath ).mkdirs();
            IndexWriter writer = new IndexWriter( directoryPath, analyzer,
                true, MaxFieldLength.UNLIMITED );
            writer.close();
        }
        directory = FSDirectory.getDirectory( directoryPath );
        if ( IndexWriter.isLocked( directory ) )
        {
            IndexWriter.unlock( directory );
        }
    }
    
    private Directory getDir() throws IOException
    {
        return this.directory;
    }
    
    private IndexWriter getWriter( boolean create ) throws IOException
    {
        return new IndexWriter( getDir(), analyzer, create,
            MaxFieldLength.UNLIMITED );
    }
    
    public void index( Node node, Uri predicate, Object literal )
    {
        index( node.getId(), predicate.getUriAsString(), literal );
    }
    
    private void index( long nodeId, String predicate, Object literal )
    {
        enqueueCommand( true, nodeId, predicate, literal );
    }
    
    private void enqueueCommand( boolean trueForIndex,
        long nodeId, String predicate, Object literal )
    {
        if ( predicateFilter != null &&
            !predicateFilter.contains( predicate ) )
        {
            return;
        }
        
        try
        {
            int key =
                neoUtil.getTransactionManager().getTransaction().hashCode();
            Collection<Object[]> commands = toIndex.get( key );
            if ( commands == null )
            {
                commands = new ArrayList<Object[]>();
                toIndex.put( key, commands );
            }
            commands.add( new Object[] {
                trueForIndex, nodeId, predicate, literal
            } );
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    protected void safeClose( Object object )
    {
        try
        {
            if ( object != null )
            {
                if ( object instanceof IndexWriter )
                {
                    ( ( IndexWriter ) object ).close();
                }
                else if ( object instanceof IndexReader )
                {
                    ( ( IndexReader ) object ).close();
                }
                else if ( object instanceof IndexSearcher )
                {
                    ( ( IndexSearcher ) object ).close();
                }
                else
                {
                    throw new RuntimeException( object.getClass().getName() );
                }
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }
    
    private void doIndex( IndexWriter writer, long nodeId, String predicate,
        Object literal )
    {
        try
        {
            Document doc = new Document();
            doc.add( new Field( KEY_ID, String.valueOf( nodeId ), Store.YES,
                Index.NOT_ANALYZED ) );
            doc.add( new Field( KEY_INDEX, getLiteralReader().read( literal ),
                Store.YES, Index.ANALYZED ) );
            doc.add( new Field( KEY_PREDICATE, predicate,
                Store.YES, Index.NOT_ANALYZED ) );
            doc.add( new Field( KEY_INDEX_SOURCE, literal.toString(),
                Store.YES, Index.NOT_ANALYZED ) );
            writer.addDocument( doc );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    public void removeIndex( Node node, Uri predicate, Object literal )
    {
        removeIndex( node.getId(), predicate.getUriAsString(), literal );
    }
    
    private void removeIndex( long nodeId, String predicate, Object literal )
    {
        enqueueCommand( false, nodeId, predicate, literal );
    }
    
    private void doRemoveIndex( IndexWriter writer,
        long nodeId, String predicate, Object literal )
    {
        try
        {
            BooleanQuery deletionQuery = new BooleanQuery();
            deletionQuery.add( new TermQuery(
                new Term( KEY_ID, String.valueOf( nodeId ) ) ), Occur.MUST );
            deletionQuery.add( new TermQuery(
                new Term( KEY_PREDICATE, predicate ) ), Occur.MUST );
            deletionQuery.add( new TermQuery(
                new Term( KEY_INDEX_SOURCE, literal.toString() ) ),
                Occur.MUST );
            
            writer.deleteDocuments( deletionQuery );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    private synchronized IndexSearcher getSearcher() throws IOException
    {
        if ( this.indexSearcher == null )
        {
            this.indexSearcher = new IndexSearcher( getDir() );
        }
        else
        {
            IndexReader reopened =
                this.indexSearcher.getIndexReader().reopen();
            if ( reopened != null )
            {
                this.indexSearcher = new IndexSearcher( reopened );
            }
        }
        return this.indexSearcher;
    }
    
    private void leaveSearcher( IndexSearcher searcher )
    {
    }
    
    public Iterable<RawQueryResult> search( String query )
    {
        return searchWithSnippets( query, 0 );
    }
    
    public Iterable<RawQueryResult> searchWithSnippets( String query,
        int snippetCountLimit )
    {
        IndexSearcher searcher = null;
        try
        {
            TemporaryLogger.Timer timer = new TemporaryLogger.Timer();
            searcher = getSearcher();
            Query q = new QueryParser( KEY_INDEX, analyzer ).parse( query );
            Hits hits = searcher.search( q, Sort.RELEVANCE );
            long searchTime = timer.lap();
            TemporaryLogger.getLogger().info( "FulltextIndex.search: " +
                "search{q:'" + query + "' time:" + searchTime +
                " hits:" + hits.length() + "}" );
            
            Highlighter highlighter = null;
            if ( snippetCountLimit > 0 )
            {
                highlighter = new Highlighter( highlightFormatter,
                    new QueryScorer( searcher.rewrite( q ) ) );
            }
            
            Iterator<RawQueryResult> resultIterator =
                new ResultIterator( hits, snippetCountLimit,
                    highlighter );
            return new IteratorAsIterable<RawQueryResult>( resultIterator );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        catch ( ParseException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            leaveSearcher( searcher );
        }
    }
    
    private class ResultIterator extends FilteringIterator<RawQueryResult>
    {
        ResultIterator( Hits hits, int snippetCountLimit,
            Highlighter highlighter )
        {
            super( new RawResultIterator( hits, snippetCountLimit,
                highlighter ) );
        }

        @Override
        protected boolean passes( RawQueryResult result )
        {
            return result != null && result != SPECIAL_FILTERING_INSTANCE;
        }
    }
    
    private static final RawQueryResult SPECIAL_FILTERING_INSTANCE =
        new RawQueryResult( null, 0, null );
    
    private class RawResultIterator extends PrefetchingIterator<RawQueryResult>
    {
        private Hits hits;
        private int hitsLength;
        private int snippetCountLimit;
        private Highlighter highlighter;
        private int counter = 0;
        private Set<Long> ids = new HashSet<Long>();
        
        private long getIdTime = 0;
        private long getSnippetTime = 0;
        private long getNodeTime = 0;
        
        RawResultIterator( Hits hits, int snippetCountLimit,
            Highlighter highlighter )
        {
            this.hits = hits;
            this.hitsLength = hits.length();
            this.snippetCountLimit = snippetCountLimit;
            this.highlighter = highlighter;
        }
        
        @Override
        protected RawQueryResult fetchNextOrNull()
        {
            int docNum = counter;
            if ( counter >= hitsLength )
            {
                TemporaryLogger.getLogger().info( "Fulltext.search DONE {" +
                    "getId:" + getIdTime + " " +
                    "getSnippet:" + getSnippetTime + " " +
                    "getNode:" + getNodeTime +
                    "}" );
                return null;
            }
            
            counter++;
            try
            {
                long t = System.currentTimeMillis();
                Document doc = hits.doc( docNum );
                long id = Long.parseLong( doc.get( KEY_ID ) );
                getIdTime += ( System.currentTimeMillis() - t );
                if ( !ids.add( id ) )
                {
                    // It's a duplicate here, probably after a crash or
                    // something
                    removeDuplicate( doc );
                    return SPECIAL_FILTERING_INSTANCE;
                }
                float score = hits.score( docNum );
                
                String snippet = null;
                t = System.currentTimeMillis();
                if ( docNum < snippetCountLimit )
                {
                    snippet = generateSnippet( doc, highlighter );
                }
                getSnippetTime += ( System.currentTimeMillis() - t );
                
                try
                {
                    t = System.currentTimeMillis();
                    Node node = neo.getNodeById( id );
                    getNodeTime += ( System.currentTimeMillis() - t );
                    return new RawQueryResult( node, score, snippet );
                }
                catch ( NotFoundException e )
                {
                    // Ok, probably index lagging a bit behind, that's all.
                    // This also effectively hides many bugs, which is a
                    // BAAD thing.
                    TemporaryLogger.getLogger().info(
                    "Fulltext index refers " +
                    "to missing node (" + id + "). This probably means " +
                    "that the indexer is lagging behind a bit. If this " +
                    "id is reported as missing a couple of more times " +
                    "then there's probably a bug and you should " +
                    "report it" );
                    return SPECIAL_FILTERING_INSTANCE;
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }
    
    private void removeDuplicate( Document doc )
    {
        long nodeId = Long.parseLong( doc.get( KEY_ID ) );
        String predicate = doc.get( KEY_PREDICATE );
        String literal = doc.get( KEY_INDEX_SOURCE );
        removeIndex( nodeId, predicate, literal );
        index( nodeId, predicate, literal );
    }
    
    private String generateSnippet( Document doc, Highlighter highlighter )
    {
        StringBuffer snippet = new StringBuffer();
        for ( Field field : doc.getFields( KEY_INDEX ) )
        {
            String text = field.stringValue();
            TokenStream tokenStream = analyzer.tokenStream( KEY_INDEX,
                new StringReader( text ) );
            try
            {
                String fragment = highlighter.getBestFragments(
                    tokenStream, text, 2, SNIPPET_DELIMITER );
                if ( snippet.length() > 0 )
                {
                    snippet.append( SNIPPET_DELIMITER );
                }
                snippet.append( fragment );
            }
            catch ( IOException e )
            {
                // TODO
                continue;
            }
        }
        return snippet.toString();
    }
    
    public boolean verify( VerificationHook hook, String queryOrNullForAll )
    {
        IndexSearcher searcher = null;
        try
        {
            searcher = new IndexSearcher( getDir() );
            Map<Status, MutableInteger> counts =
                new HashMap<Status, MutableInteger>();
            int maxDoc = 0;
            final IndexReader reader = searcher.getIndexReader();
            Iterator<Integer> hitsIterator = null;
            if ( queryOrNullForAll == null )
            {
                maxDoc = reader.maxDoc();
                hitsIterator = new PrefetchingIterator<Integer>()
                {
                    private int limit = reader.maxDoc();
                    private int counter;
                    
                    @Override
                    protected Integer fetchNextOrNull()
                    {
                        int c = counter++;
                        return c < limit ? c : null;
                    }
                };
            }
            else
            {
                Query q = new QueryParser( KEY_INDEX, analyzer ).parse(
                    queryOrNullForAll );
                final Hits hits = searcher.search( q, Sort.RELEVANCE );
                maxDoc = hits.length();
                hitsIterator = new PrefetchingIterator<Integer>()
                {
                    private int counter;
                    
                    @Override
                    protected Integer fetchNextOrNull()
                    {
                        try
                        {
                            int c = counter++;
                            return c < hits.length() ? hits.id( c ) : null;
                        }
                        catch ( IOException e )
                        {
                            throw new RuntimeException( e );
                        }
                    }
                };
            }
            
            hook.verificationStarting( maxDoc );
            while ( hitsIterator.hasNext() )
            {
                int docId = hitsIterator.next();
                if ( reader.isDeleted( docId ) )
                {
                    hook.oneWasSkipped();
                    continue;
                }
                
                Document doc = reader.document( docId );
                long nodeId = Long.parseLong( doc.get( KEY_ID ) );
                Status status = hook.verify( nodeId,
                    doc.get( KEY_PREDICATE ), doc.get( KEY_INDEX_SOURCE ) );
                MutableInteger count = counts.get( status );
                if ( count == null )
                {
                    count = new MutableInteger();
                    counts.put( status, count );
                }
                count.value++;
            }
            
            Map<Status, Integer> resultCounts = new HashMap<Status, Integer>();
            int errors = 0;
            for ( Map.Entry<Status, MutableInteger> count :
                counts.entrySet() )
            {
                resultCounts.put( count.getKey(), count.getValue().value );
                errors += ( count.getKey() == Status.OK ? 0 :
                    count.getValue().value );
            }
            hook.verificationCompleted( resultCounts );
            return errors == 0;
        }
        catch ( ParseException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            safeClose( searcher );
        }
    }
    
    private static class MutableInteger
    {
        private int value;
    }
    
    public LiteralReader getLiteralReader()
    {
        return this.literalReader;
    }
    
    public void setLiteralReader( LiteralReader reader )
    {
        this.literalReader = reader;
    }
    
    public void end( boolean commit )
    {
        try
        {
            end( neoUtil.getTransactionManager().getTransaction().hashCode(),
                commit );
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    public void end( int txId, boolean commit )
    {
        Collection<Object[]> commands = toIndex.remove( txId );
        if ( commands == null || !commit )
        {
            return;
        }
        
        for ( Object[] command : commands )
        {
            this.indexingQueue.add( command );
            this.indexingThread.hasItems = true;
        }
    }
    
    public boolean queueIsEmpty()
    {
        return !this.indexingThread.hasItems;
    }
    
    public void shutDown()
    {
//        TemporaryLogger.getLogger().info( getClass().getName() +
//            " shutDown called", new Exception() );
        internalShutDown();
    }
    
    private void internalShutDown()
    {
        indexingThread.halt();
        try
        {
            indexingThread.join();
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        
        indexingQueue.close();
        try
        {
            directory.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        safeClose( this.indexSearcher );
        this.indexSearcher = null;
    }
    
    private class IndexingThread extends Thread
    {
        private boolean halted;
        private boolean hasItems;
        private IndexWriter writer;
        private Collection<Entry> entriesToComplete = new ArrayList<Entry>();
        
        private void halt()
        {
            this.halted = true;
        }
        
        @Override
        public void run()
        {
            while ( !halted )
            {
                try
                {
                    hasItems = indexingQueue.hasNext();
                    while ( !halted && hasItems )
                    {
                        Entry entry = indexingQueue.next();
                        Object[] data = entry.data();
                        ensureWriters();
                        if ( ( Boolean ) data[ 0 ] )
                        {
                            doIndex( writer, ( Long ) data[ 1 ],
                                ( String ) data[ 2 ], data[ 3 ] );
                        }
                        else
                        {
                            doRemoveIndex( writer, ( Long ) data[ 1 ],
                                ( String ) data[ 2 ], data[ 3 ] );
                        }
                        entriesToComplete.add( entry );
                        
                        if ( entriesToComplete.size() >= BATCH_SIZE ||
                            !indexingQueue.hasNext() )
                        {
                            flushEntries();
                        }
                        hasItems = indexingQueue.hasNext();
                    }
                    
                    // This is so that it flushes if the indexer gets halted.
                    flushEntries();
                    
                    try
                    {
                        long time = System.currentTimeMillis();
                        while ( !halted &&
                            System.currentTimeMillis() - time < 100 )
                        {
                            hasItems = indexingQueue.hasNext();
                            Thread.sleep( 20 );
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.interrupted();
                    }
                }
                catch ( Throwable t )
                {
                    t.printStackTrace();
                }
            }
        }
        
        private void ensureWriters() throws Exception
        {
            if ( writer == null )
            {
                writer = getWriter( false );
                writer.setMaxBufferedDocs( BATCH_SIZE * 2 );
                writer.setMaxBufferedDeleteTerms( BATCH_SIZE * 2 );
            }
        }
        
        private void flushEntries()
        {
            if ( writer == null )
            {
                return;
            }
            
            safeClose( writer );
            writer = null;
//            try
//            {
//                writer.commit();
//                
//            }
//            catch ( IOException e )
//            {
//                TemporaryLogger.getLogger().info(
//                    "Couldn't commit fulltext index writer ", e );
//                safeClose( writer );
//                writer = null;
//            }
            indexingQueue.markAsCompleted( entriesToComplete.toArray(
                new Entry[ entriesToComplete.size() ] ) );
            entriesToComplete.clear();
        }
    }
    
    private void delete()
    {
        deleteDir( new File( directoryPath ) );
        new File( queuePath ).delete();
    }
    
    protected void deleteDir( File dir )
    {
        if ( !dir.exists() )
        {
            return;
        }
        
        for ( File child : dir.listFiles() )
        {
            if ( child.isFile() )
            {
                child.delete();
            }
            else
            {
                deleteDir( child );
            }
        }
        dir.delete();
    }
}
