package org.neo4j.rdf.store.testrepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.AbstractNode;

class AbstractNodeTestImpl implements AbstractNode
{
    private final String uriOrNull;
    private final Map<String, Object> propertyMap =
        Collections.synchronizedMap( new HashMap<String, Object>() );
    
    AbstractNodeTestImpl( String uriOrNull )
    {
        this.uriOrNull = uriOrNull;
    }
    
    void addProperty( String key, Object value )
    {
        propertyMap.put( key, value );
    }
    
    public Map<String, Object> properties()
    {
        return this.propertyMap;
    }
    
    public Uri getUriOrNull()
    {
        return new Uri()
        {
            public String uriAsString()
            {
                return uriOrNull;
            }                
        };
    }
}