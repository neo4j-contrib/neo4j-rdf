package org.neo4j.rdf.store.representation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.Node;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.UriImpl;

/**
 * Represents a more simple abstraction of a {@link Node}.
 */
public class AbstractNode extends AbstractElement
{
    private final String uriOrNull;
    private final Map<String, Object> propertyMap = Collections
        .synchronizedMap( new HashMap<String, Object>() );

    /**
     * @param uriOrNull the URI of this node, or {@code null} if it's a
     * blank node.
     */
    public AbstractNode( String uriOrNull )
    {
        this.uriOrNull = uriOrNull;
    }

    /**
     * Adds a property to this node.
     * @param key the property key.
     * @param value the property value.
     */
    public void addProperty( String key, Object value )
    {
        propertyMap.put( key, value );
    }

    /**
     * @return all the properties set with {@link #addProperty(String, Object)}.
     */
    public Map<String, Object> properties()
    {
        return this.propertyMap;
    }

    /**
     * @return the {@link Uri} which this {@link AbstractNode} was constructed
     * with or {@code null} if it's a blank node.
     */
    public Uri getUriOrNull()
    {
        return uriOrNull == null ? null : new UriImpl( uriOrNull );
    }
}
