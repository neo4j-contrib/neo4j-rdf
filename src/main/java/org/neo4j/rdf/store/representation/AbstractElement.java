package org.neo4j.rdf.store.representation;

import java.util.Collections;
import java.util.HashMap;
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
    private final Map<String, String> infoMap = Collections
        .synchronizedMap( new HashMap<String, String>() );

    /**
     * Adds data to the "lookup info" data holder. 
     * @param key the data key.
     * @param value the value.
     */
    public void addLookupInfo( String key, String value )
    {
        this.infoMap.put( key, value );
    }

    /**
     * Looks up a value set with {@link #addLookupInfo(String, String)}.
     * @param key the data key.
     * @return the value for the data key, or {@code null} if there were
     * no value associated with {@code key}.
     */
    public String lookupInfo( String key )
    {
        return this.infoMap.get( key );
    }
}
