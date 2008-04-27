package org.neo4j.rdf.model;

/**
 * A wildcard placeholder for an RDF data type, used with
 * {@link WildcardStatement} to represent variable wildcard patterns
 * for the system to match. It has an optional name.
 */
public class Wildcard implements Value
{
    private final String name;
    
    public Wildcard()
    {
        this.name = null;
    }  
    
    public Wildcard( String name )
    {
        this.name = name;
    }
    
    /**
     * The name of the wildcard, or <code>null</code>.
     * @return the name of the wildcard of <code>null</code>
     */
    public String getVariableName()
    {
        return name;
    }
    
    /**
     * Returns <code>true</code> (a Wildcard is, surprisingly, a wildcard)
     * @return <code>true</code>
     */
    public boolean isWildcard()
    {        
        return true;
    }   

    @Override
    public String toString()
    {
        return "Wildcard[" + name == null ? "no name" : name + "]";
    }
}