package org.neo4j.rdf.model;

/**
 * Implementation of {@link TripleObject} as a literal.
 */
public class TripleObjectLiteral implements TripleObject
{
    private Object literalValue;

    /**
     * @param literalValue the literal value.
     */
    public TripleObjectLiteral( Object literalValue )
    {
        this.literalValue = literalValue;
    }

    public Object getLiteralValueOrNull()
    {
        return this.literalValue;
    }

    public Uri getResourceOrNull()
    {
        return null;
    }

    public boolean isObjectProperty()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "ObjectLiteral[" + getLiteralValueOrNull() + "]";
    }
}
