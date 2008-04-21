package org.neo4j.rdf.store.representation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.UriImpl;

public class AbstractNode extends AbstractElement
{
    private final String uriOrNull;
    private final Map<String, Object> propertyMap =
        Collections.synchronizedMap( new HashMap<String, Object>() );
    
    public AbstractNode( String uriOrNull )
    {
        this.uriOrNull = uriOrNull;
    }
    
    public void addProperty( String key, Object value )
    {
        propertyMap.put( key, value );
    }
    
    public Map<String, Object> properties()
    {
        return this.propertyMap;
    }
    
    public Uri getUriOrNull()
    {
    	return uriOrNull == null ? null : new UriImpl( uriOrNull );
    }
}
