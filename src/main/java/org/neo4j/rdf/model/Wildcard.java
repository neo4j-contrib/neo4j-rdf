package org.neo4j.rdf.model;

/**
 * A wildcard placeholder for an RDF data type, used with
 * {@link WildcardEnabledStatement} represent variable wildcard patterns
 * for the system to match.
 */
public class Wildcard implements Value
{
    private final String name;
    public Wildcard( String name )
    {
        this.name = name;
    }
    public String getVariableName()
    {
        return name;
    }
    
    @Override
    public String toString()
    {
        return "Wildcard[" + name + "]";
    }
}