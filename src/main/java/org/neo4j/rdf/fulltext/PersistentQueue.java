package org.neo4j.rdf.fulltext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persistent queue of fundamental values (boolean, byte, int, char, short,
 * long, float, double)
 * 
 * Each entry is:
 * byte:	ENTRY_STATE (completed, not_completed)
 * int:		ENTRY_SIZE
 * ...:		USER_DATA
 * 
 * Only one reader is allowed, but many writers to this queue
 */
public class PersistentQueue implements Iterator<PersistentQueue.Entry>
{
    // byte:STATE, int:ENTRY_SIZE
    private static final int HEADER_SIZE = 1 + Integer.SIZE / 8;
    private static final byte NOT_COMPLETED = 0;
    private static final byte COMPLETED = 1;
    
    private File file;
    private ByteBuffer internalBuffer;
    private int bufferSize;
    private FileChannel channel;
    private Entry previousEntry;
    private Entry nextEntry;
    private boolean autoCompleteEntries = true;
    private AtomicInteger numberOfEntriesReadButNotYetCompleted =
        new AtomicInteger();
    private AtomicInteger totalQueueIndex = new AtomicInteger();
    private boolean recoveryWasNeeded;
    
    public PersistentQueue( File file )
    {
        this.file = file;
        getBuffer( 500 );
        try
        {
            openOrCreate();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    public void setAutoCompleteEntries( boolean autoComplete )
    {
        this.autoCompleteEntries = autoComplete;
    }
    
    private ByteBuffer getBuffer( int atLeastOfSize )
    {
        if ( atLeastOfSize > 1000000 )
        {
            throw new RuntimeException( "Requested a very big buffer " +
                atLeastOfSize + ", can't be right" );
        }
        
        if ( atLeastOfSize > bufferSize )
        {
            bufferSize = atLeastOfSize * 2;
            internalBuffer = ByteBuffer.allocateDirect( bufferSize );
        }
        return internalBuffer;
    }
    
    private int getFundamentalValueSize( Object object )
    {
        return FundamentalTypeNioUtil.getInstance(
            object.getClass() ).size( object );
    }
    
    private void openOrCreate() throws IOException
    {
        checkConsistency();
        openChannel();
    }
    
    private void openChannel() throws IOException
    {
        channel = new RandomAccessFile( file, "rw" ).getChannel();
    }
    
    private void closeChannel()
    {
        if ( channel != null )
        {
            try
            {
                channel.close();
            }
            catch ( IOException e )
            {
                // It's ok
                System.out.println( "Couldn't close channel" );
            }
        }
    }
    
    private void checkConsistency() throws IOException
    {
        openChannel();
        try
        {
            if ( channel.size() == 0 )
            {
                return;
            }
            
            long position = 0;
            while ( true )
            {
                try
                {
                    Entry entry = tryToFindNext();
                    if ( entry == null )
                    {
                        // Alright, no problems
                        break;
                    }
                }
                catch ( IOException e )
                {
                    // Somethings' wrong with the file, truncate here
                    closeChannel();
                    openChannel();
                    channel.truncate( position );
                    recoveryWasNeeded = true;
                    break;
                }
                position = channel.position();
            }
        }
        finally
        {
            closeChannel();
        }
    }
    
    public boolean recoveryWasNeeded()
    {
        return recoveryWasNeeded;
    }
    
    public synchronized void add( Object... entryData )
    {
        try
        {
            long position = channel.position();
            try
            {
                channel.position( channel.size() );
                channel.write( fillBuffer( entryData ) );
                channel.force( true );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
            finally
            {
                channel.position( position );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    private int calculateDataSize( Object[] entryData )
    {
        int size = 0;
        for ( Object object : entryData )
        {
            // one byte for which type it is (boolean, byte, short...)
            size += ( 1 + getFundamentalValueSize( object ) );
        }
        return size;
    }
    
    private ByteBuffer fillBuffer( Object[] entryData )
    {
        int entrySize = calculateDataSize( entryData );
        int totalSize = HEADER_SIZE + entrySize;
        ByteBuffer buffer = getBuffer( totalSize );
        buffer.clear();
        buffer.limit( totalSize );
        buffer.put( NOT_COMPLETED );
        buffer.putInt( entrySize );
        for ( Object data : entryData )
        {
            Class<? extends Object> cls = data.getClass();
            FundamentalTypeNioUtil typeUtil =
                FundamentalTypeNioUtil.getInstance( cls );
            buffer.put( typeUtil.byteKey() );
            typeUtil.putIntoByteBuffer( buffer, data );
        }
        buffer.flip();
        return buffer;
    }
    
    public synchronized void markAsCompleted( Entry... entries )
    {
        try
        {
            long position = channel.position();
            try
            {
                for ( Entry entry : entries )
                {
                    channel.position( entry.position() );
                    if ( readNextEntryHeader( true ).state == COMPLETED )
                    {
                        continue;
                    }
                    
                    ByteBuffer buffer = getBuffer( HEADER_SIZE );
                    buffer.clear();
                    buffer.limit( 1 );
                    buffer.put( COMPLETED );
                    buffer.flip();
                    channel.write( buffer );
                    numberOfEntriesReadButNotYetCompleted.decrementAndGet();
                }
                channel.force( false );
            }
            finally
            {
                channel.position( position );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    public boolean hasNext()
    {
        if ( nextEntry != null )
        {
            return true;
        }
        
        if ( previousEntry != null )
        {
            if ( autoCompleteEntries )
            {
                markAsCompleted( previousEntry );
            }
            previousEntry = null;
        }
        
        try
        {
            nextEntry = tryToFindNext();
            if ( nextEntry != null )
            {
                numberOfEntriesReadButNotYetCompleted.incrementAndGet();
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        return nextEntry != null;
    }
    
    private synchronized EntryHeader readNextEntryHeader(
        boolean restorePositionAfterRead ) throws IOException
    {
        long positionBeforeRead = channel.position();
        try
        {
            ByteBuffer buffer = getBuffer( HEADER_SIZE );
            buffer.clear();
            buffer.limit( HEADER_SIZE );
            long bytesRead = channel.read( buffer );
            if ( bytesRead < HEADER_SIZE )
            {
                throw new IOException( "Invalid header:" + bytesRead +
                " bytes" );
            }
            buffer.flip();
            byte state = buffer.get();
            int entrySize = buffer.getInt();
            return new EntryHeader( state, entrySize );
        }
        finally
        {
            if ( restorePositionAfterRead )
            {
                channel.position( positionBeforeRead );
            }
        }
    }
    
    private synchronized Entry tryToFindNext() throws IOException
    {
        Entry result = null;
        while ( result == null && channel.position() < channel.size() )
        {
            long positionBeforeRead = channel.position();
            EntryHeader header = readNextEntryHeader( false );
            totalQueueIndex.incrementAndGet();
            if ( header.state == NOT_COMPLETED )
            {
                ByteBuffer buffer = getBuffer( header.entrySize );
                buffer.clear();
                buffer.limit( header.entrySize );
                channel.read( buffer );
                buffer.flip();
                Object[] data = readBuffer( buffer );
                long entryPosition = positionBeforeRead;
                result = new Entry( data, entryPosition );
            }
            else if ( header.state == COMPLETED )
            {
                channel.position( channel.position() + header.entrySize );
                continue;
            }
            else
            {
                throw new IOException( "Invalid entry state " + header.state );
            }
        }
        return result;
    }
    
    private Object[] readBuffer( ByteBuffer buffer )
    {
        List<Object> list = new ArrayList<Object>();
        while ( buffer.position() < buffer.limit() )
        {
            FundamentalTypeNioUtil typeUtil =
                FundamentalTypeNioUtil.getInstance( buffer.get() );
            Object object = typeUtil.readFromByteBuffer( buffer );
            list.add( object );
        }
        return list.toArray();
    }
    
    public Entry next()
    {
        if ( !hasNext() )
        {
            throw new NoSuchElementException();
        }
        Entry result = nextEntry;
        previousEntry = nextEntry;
        nextEntry = null;
        return result;
    }
    
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
    
    public int getTotalQueuePosition()
    {
        return this.totalQueueIndex.get();
    }
    
    /**
     * Returns <code>true</code> if the queue has no more incompleted entries
     * and the backing file was deleted.
     */
    public boolean close()
    {
        boolean hasNext = hasNext();
        boolean hasIncompletedEntries =
            numberOfEntriesReadButNotYetCompleted.get() > 0;
        boolean keepFile = hasNext || hasIncompletedEntries;
        closeChannel();
        if ( !keepFile )
        {
            deleteBackingFile();
        }
        return !keepFile;
    }
    
    protected void deleteBackingFile()
    {
        // We delete the backing file if all the entries in it are completed
        if ( !file.delete() )
        {
            file.deleteOnExit();
        }
    }
    
    public static class Entry
    {
        private Object[] data;
        private long position;
        
        private Entry( Object[] data, long position )
        {
            this.data = data;
            this.position = position;
        }
        
        public Object[] data()
        {
            return this.data;
        }
        
        private long position()
        {
            return this.position;
        }
    }
    
    private static class EntryHeader
    {
        private byte state;
        private int entrySize;
        
        private EntryHeader( byte state, int entrySize )
        {
            this.state = state;
            this.entrySize = entrySize;
        }
    }
}
