package org.neo4j.rdf.newmodel;

public class Literal implements Value
{
    private final Object value;
    
    public Literal( Object value )
    {
        this.value = value;
    }
    
    public Object getValue()
    {
        return this.value;
    }   
}