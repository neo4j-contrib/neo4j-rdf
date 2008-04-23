package org.neo4j.rdf.store.representation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Abstract super class for {@link AbstractNode} and
 * {@link AbstractRelationship}.
 * 
 * The {@link #lookupInfo(String)} concept if like a means of communication
 * between an implementation of {@link RdfRepresentationStrategy} and
 * an implementation of {@link AsrExecutor}.
 */
public abstract class AbstractElement
{
    private final Map<String, Collection<Object>> propertyMap = Collections
        .synchronizedMap( new HashMap<String, Collection<Object>>() );
    private final Map<String, Object> infoMap = Collections
        .synchronizedMap( new HashMap<String, Object>() );

    /**
     * Adds a property to this node.
     * @param key the property key.
     * @param value the property value.
     */
    public void addProperty( String key, Object value )
    {
        addToMap( propertyMap, key, value );
    }
    
    private <T> void addToMap( Map<String, Collection<T>> map,
        String key, T value )
    {
        Collection<T> collection = map.get( key );
        if ( collection == null )
        {
            collection = new HashSet<T>();
            map.put( key, collection );
        }
        collection.add( value );
    }

    /**
     * @return all the properties set with {@link #addProperty(String, Object)}.
     */
    public Map<String, Collection<Object>> properties()
    {
        return this.propertyMap;
    }
    
    /**
     * Adds data to the "lookup info" data holder. 
     * @param key the data key.
     * @param value the value.
     */
    public void addLookupInfo( String key, Object value )
    {
        this.infoMap.put( key, value );
    }

    /**
     * Looks up a value set with {@link #addLookupInfo(String, Object)}.
     * @param key the data key.
     * @return the value for the data key, or {@code null} if there were
     * no value associated with {@code key}.
     */
    public Object lookupInfo( String key )
    {
        return this.infoMap.get( key );
    }
}
