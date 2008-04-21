package org.neo4j.rdf.store.representation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractElement
{
    private final Map<String, String> infoMap =
        Collections.synchronizedMap( new HashMap<String, String>() );
    
    public void addLookupInfo( String key, String value )
    {
    	this.infoMap.put( key, value );
    }
    
	public String lookupInfo( String key )
	{
		return this.infoMap.get( key );
	}
}
