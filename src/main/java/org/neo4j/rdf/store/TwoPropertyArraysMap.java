package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.util.NeoPropertyArraySet;

/**
 * Mm.
 */
class TwoPropertyArraysMap implements Map<Object, Collection<String>>
{
    private static final String DELIMITER = "š";
    
    private Node node;
    private String leftKey;
    private String rightKey;
    private List<Object> leftSet;
    private List<String> rightSet;
    
    /**
     * Shut up
     * @param neo
     * @param node
     * @param leftKey
     * @param rightKey
     */
    public TwoPropertyArraysMap( NeoService neo, Node node, String leftKey,
        String rightKey )
    {
        this.node = node;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.leftSet = new NeoPropertyArraySet<Object>( neo, node, leftKey );
        this.rightSet = new NeoPropertyArraySet<String>( neo, node, rightKey );
    }
    
    public void clear()
    {
        node.removeProperty( leftKey );
        node.removeProperty( rightKey );
    }

    public boolean containsKey( Object key )
    {
        return this.leftSet.contains( key );
    }

    public boolean containsValue( Object value )
    {
        throw new UnsupportedOperationException();
    }

    public Set<Map.Entry<Object, Collection<String>>> entrySet()
    {
        throw new UnsupportedOperationException();
    }
    
    private Collection<String> tokenize( String string )
    {
        Collection<String> list = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer( string, DELIMITER );
        while ( tokenizer.hasMoreTokens() )
        {
            list.add( tokenizer.nextToken() );
        }
        return list;
    }
    
    private String join( Collection<String> strings )
    {
        StringBuffer buffer = new StringBuffer();
        for ( String string : strings )
        {
            buffer.append( string + DELIMITER );
        }
        return buffer.toString();
    }

    public Collection<String> get( Object key )
    {
        int theIndex = leftSet.indexOf( key );
        return theIndex == -1 ? null : tokenize( rightSet.get( theIndex ) );
    }

    public boolean isEmpty()
    {
        return leftSet.isEmpty();
    }

    public Set<Object> keySet()
    {
        throw new UnsupportedOperationException();
    }

    public Collection<String> put( Object key, Collection<String> value )
    {
        int index = leftSet.indexOf( key );
        Collection<String> oldValues = null;
        if ( index == -1 )
        {
            leftSet.add( key );
            rightSet.add( join( value ) );
        }
        else
        {
            String oldString = rightSet.set( index, join( value ) );
            oldValues = oldString == null ? null : tokenize( oldString );
        }
        return oldValues;
    }
    
    /**
     * Yes dude.
     * @param key
     * @param value
     * @return added or not.
     */
    public boolean addOne( Object key, String value )
    {
        int index = leftSet.indexOf( key );
        boolean added = false;
        if ( index == -1 )
        {
            put( key, Arrays.asList( value ) );
            added = true;
        }
        else
        {
            added = get( key ).add( value );
        }
        return added;
    }
    
    /**
     * Yes dude.
     * @param key
     * @param value
     * @return removed or not.
     */
    public boolean removeOne( Object key, String value )
    {
        int index = leftSet.indexOf( key );
        boolean removed = false;
        if ( index != -1 )
        {
            removed = get( key ).remove( value );
        }
        return removed;
    }

    public void putAll(
        Map<? extends Object, ? extends Collection<String>> map )
    {
        throw new UnsupportedOperationException();
    }

    public Collection<String> remove( Object key )
    {
        int index = leftSet.indexOf( key );
        Collection<String> result = null;
        if ( index != -1 )
        {
            String resultString = rightSet.get( index );
            result = resultString == null ? null : tokenize( resultString );
        }
        return result;
    }

    public int size()
    {
        return leftSet.size();
    }

    public Collection<Collection<String>> values()
    {
        throw new UnsupportedOperationException();
    }
}
