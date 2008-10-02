package org.neo4j.rdf.fulltext;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class for dealing with fundamental types and nio, basically
 * asking for size, putting into a byte buffer, reading from a byte buffer
 * a.s.o. So instead of having:
 * 
 * if ( cls.equals( Boolean.class ) )
 * 		// buffer.put( .... )
 * else if ( cls.equals( Integer.class ) )
 * 		// buffer.putInt( .... )
 * 
 * And have that in at least two places (when you deal with these things)
 * you could instead use this util class to simplify things and reduce bugs
 */
public abstract class FundamentalTypeNioUtil
{
    private static Map<Class<? extends Object>, FundamentalTypeNioUtil> byClass
        = new HashMap<Class<? extends Object>, FundamentalTypeNioUtil>();
    private static Map<Byte, FundamentalTypeNioUtil> byByteKey =
        new HashMap<Byte, FundamentalTypeNioUtil>();
    
    static
    {
        new BooleanType();
        new CharacterType();
        new ByteType();
        new ShortType();
        new IntegerType();
        new LongType();
        new FloatType();
        new DoubleType();
        new StringType();
    }
    
    private Class<? extends Object> cls;
    private byte byteKey;
    
    private FundamentalTypeNioUtil( Class<? extends Object> cls, byte byteKey )
    {
        this.cls = cls;
        this.byteKey = byteKey;
        
        byClass.put( cls, this );
        byByteKey.put( byteKey, this );
    }
    
    public static FundamentalTypeNioUtil getInstance(
        Class<? extends Object> cls )
    {
        if ( !byClass.containsKey( cls ) )
        {
            throw new IllegalArgumentException( "Invalid class " + cls );
        }
        return byClass.get( cls );
    }
    
    public static FundamentalTypeNioUtil getInstance( byte byteKey )
    {
        if ( !byByteKey.containsKey( byteKey ) )
        {
            throw new IllegalArgumentException( "Invalid byte key " + byteKey );
        }
        return byByteKey.get( byteKey );
    }
    
    public Class<? extends Object> typeClass()
    {
        return this.cls;
    }
    
    public byte byteKey()
    {
        return this.byteKey;
    }
    
    public abstract int size( Object value );
    
    public abstract void putIntoByteBuffer( ByteBuffer buffer, Object value );
    
    public abstract Object readFromByteBuffer( ByteBuffer buffer );
    
    private static class BooleanType extends FundamentalTypeNioUtil
    {
        public BooleanType()
        {
            super( Boolean.class, ( byte ) 1 );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            byte byteValue = ( Boolean ) value ? ( byte ) 1 : ( byte ) 0;
            buffer.put( byteValue );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            byte value = buffer.get();
            return value != 0;
        }
        
        @Override
        public int size( Object value )
        {
            return 1;
        }
    }
    
    private static abstract class NumberType extends FundamentalTypeNioUtil
    {
        private int size;
        
        public NumberType( Class<? extends Object> cls, byte byteKey,
            int numberOfBits )
        {
            super( cls, byteKey );
            this.size = numberOfBits / 8;
            assert this.size > 0;
        }
        
        @Override
        public int size( Object value )
        {
            return size;
        }
    }
    
    private static class CharacterType extends NumberType
    {
        public CharacterType()
        {
            super( Character.class, ( byte ) 2, Character.SIZE );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            buffer.putChar( ( Character ) value );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            return buffer.getChar();
        }
    }
    
    private static class ByteType extends NumberType
    {
        public ByteType()
        {
            super( Byte.class, ( byte ) 3, Byte.SIZE );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            buffer.put( ( ( Number ) value ).byteValue() );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            return buffer.get();
        }
    }
    
    private static class ShortType extends NumberType
    {
        public ShortType()
        {
            super( Short.class, ( byte ) 4, Short.SIZE );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            buffer.putShort( ( ( Number ) value ).shortValue() );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            return buffer.getShort();
        }
    }
    
    private static class IntegerType extends NumberType
    {
        public IntegerType()
        {
            super( Integer.class, ( byte ) 5, Integer.SIZE );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            buffer.putInt( ( ( Number ) value ).intValue() );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            return buffer.getInt();
        }
    }
    
    private static class LongType extends NumberType
    {
        public LongType()
        {
            super( Long.class, ( byte ) 6, Long.SIZE );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            buffer.putLong( ( ( Number ) value ).longValue() );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            return buffer.getLong();
        }
    }
    
    private static class FloatType extends NumberType
    {
        public FloatType()
        {
            super( Float.class, ( byte ) 7, Float.SIZE );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            buffer.putFloat( ( ( Number ) value ).floatValue() );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            return buffer.getFloat();
        }
    }
    
    private static class DoubleType extends NumberType
    {
        public DoubleType()
        {
            super( Double.class, ( byte ) 8, Double.SIZE );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            buffer.putDouble( ( ( Number ) value ).doubleValue() );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            return buffer.getDouble();
        }
    }
    
    private static class StringType extends FundamentalTypeNioUtil
    {
        public StringType()
        {
            super( String.class, ( byte ) 9 );
        }
        
        @Override
        public void putIntoByteBuffer( ByteBuffer buffer, Object value )
        {
            String string = ( String ) value;
            byte[] bytes = string.getBytes();
            buffer.putInt( bytes.length );
            buffer.put( bytes );
        }
        
        @Override
        public Object readFromByteBuffer( ByteBuffer buffer )
        {
            int length = buffer.getInt();
            byte[] bytes = new byte[ length ];
            buffer.get( bytes );
            return new String( bytes );
        }
        
        @Override
        public int size( Object value )
        {
            return ( Integer.SIZE / 8 ) +
            ( ( String ) value ).getBytes().length;
        }
    }
}
