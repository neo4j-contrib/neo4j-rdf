package org.neo4j.rdf.store.representation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Abstract super class for {@link AbstractNode} and
 * {@link AbstractRelationship}.
 * 
 * The {@link #getExecutorInfo(String)} concept is like a means of communication
 * between an implementation of {@link RepresentationStrategy} and
 * an implementation of {@link RepresentationExecutor}.
 */
public abstract class AbstractElement
{
    private final Map<String, Collection<Object>> propertyMap = 
        new HashMap<String, Collection<Object>>();
    private final Map<String, Object> executorInfoMap = 
        new HashMap<String, Object>();

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
     * Adds data to the "executor info" data holder that is optionally read
     * by an executor for metadata (guidelines, policies, etc) about this node.  
     * @param key the data key.
     * @param value the value.
     */
    public void addExecutorInfo( String key, Object value )
    {
        this.executorInfoMap.put( key, value );
    }

    /**
     * Looks up a value set with {@link #addExecutorInfo(String, Object)}.
     * @param key the data key.
     * @return the value for the data key, or {@code null} if there were
     * no value associated with {@code key}.
     */
    public Object getExecutorInfo( String key )
    {
        return this.executorInfoMap.get( key );
    }
}
