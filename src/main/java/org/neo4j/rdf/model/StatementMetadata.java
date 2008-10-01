package org.neo4j.rdf.model;

import org.neo4j.api.core.PropertyContainer;
import org.neo4j.rdf.store.RdfStore;

/**
 * A hook for a statement which provides metadata editing and reading
 * capabilities. A {@link StatementMetadata} implementation is typically
 * provided by an {@link RdfStore} implementation and supplied to
 * each {@link CompleteStatement} in f.ex
 * {@link RdfStore#getStatements(WildcardStatement, boolean)}.
 */
public interface StatementMetadata
{
    /**
     * Returns wether or not a certain metadata exists.
     * @param key the metadata key, typically a URI.
     * @return <code>true</code> if the metadata exists, otherwise
     * <code>false</code>.
     */
    boolean has( String key );
    
    /**
     * Returns the metadata value (as a {@link Literal}) for a certain key.
     * Throws a runtime exception if the specific metadata doesn't exist.
     * @param key the metadata key, typically a URI.
     * @return the metadata value for the given key, or throws exception
     * if if doesn't exist.
     */
    Literal get( String key );
    
    /**
     * Sets a metadata value for a certain key. Accepted values are those
     * that a neo {@link PropertyContainer} accepts.
     * @param key the metadata key, typically a URI.
     * @param value the {@link Literal} value which will be associated with
     * the key.
     */
    void set( String key, Literal value );
    
    /**
     * Removes the metadata value for a certain key. Throws a runtime exception
     * if the specific metadata doesn't exist.
     * @param key the metadata key, typically a URI.
     */
    void remove( String key );
    
    /**
     * @return all metadata keys for this statement.
     */
    Iterable<String> getKeys();
}
