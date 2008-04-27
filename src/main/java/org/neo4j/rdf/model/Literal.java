package org.neo4j.rdf.model;

import java.text.ParseException;

import org.neo4j.neometa.structure.RdfUtil;

/**
 * A literal value, i.e. String, int, byte, etc.
 *
 * Cases:
 *  o value instance of Primitive (not String)
 *      -> just keep it
 *  o value is String and a datatype is supplied
 *      -> convert it if the datatype if supported, otherwise throw
 *  o value is String and no datatype is supplied
 *      -> check in meta model (if there is a meta) and convert it later (add,get)
 *
 */
public class Literal implements Value
{
    private final Object value;
    private final Uri datatype;
    private final String language;

    // TODO: We need to detect what data type people throw in here and if
    // it's something we support natively (i.e. Java's primitive types) then
    // we make sure we convert it properly... or else people will just embed
    // their longs (for example) as strings, with datatype xsd:long. Ugly,
    // but that's how for example SAIL works. :(

    public Literal( Object value )
    {
        this( value, null, null );
    }

    public Literal( Object value, Uri datatype )
    {
        this( value, datatype, null );
    }

    public Literal( Object value, Uri datatype, String language )
    {
        this.datatype = datatype;
        this.language = language;

        Object tempraryValue = value;
        if ( !( value instanceof String ) )
        {
            // Assume primitive!
            // Just keep it as is
        }
        else if ( value instanceof String )
        {
            if ( datatype != null )
            {
                // Convert according to datatype
                tempraryValue = tryConvertValue( ( String ) value, datatype );
            }
            else
            {
                // Let it be for the store/meta to convert later
            }
        }

        this.value = tempraryValue;
    }

    private static Object tryConvertValue( String value, Uri datatype )
    {
        try
        {
            return RdfUtil.getRealValue( datatype.getUriAsString(), value );
        }
        catch ( ParseException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * The literal value of this literal
     * @return the value
     */
    public Object getValue()
    {
        return this.value;
    }

    /**
     * The optional data type of this literal
     * @return the optional data type of this literal
     */
    public Uri getDatatype()
    {
        return this.datatype;
    }

    /**
     * The optional language tag for this literal
     * @return the optional language tag for this literal
     */
    public String getLanguage()
    {
        return this.language;
    }

    /**
     * Returns <code>false</code> (a literal is not a wildcard).
     * @return <code>false</code>
     */
    public boolean isWildcard()
    {
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return getValue().hashCode(); 
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o instanceof Literal )
        {
            return getValue().equals( ( ( Literal ) o ).getValue() );
        }
        return false;
    }
    
   @Override
    public String toString()
    {
        return "Literal[" + this.value + "]";
    }
}