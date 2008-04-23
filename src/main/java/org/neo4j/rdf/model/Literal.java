package org.neo4j.rdf.model;

/**
 * A literal value, i.e. String, int, byte, etc.
 */
public class Literal implements Value
{
    private final Object value;
    
    public Literal( Object value )
    {
        this.value = value;
    }
    
    /**
     * The literal value of this literal 
     * @return the value
     */
    public Object getValue()
    {
        return this.value;
    }
    
    @Override
    public String toString()
    {
        return "Literal[" + this.value + "]";
    }
}