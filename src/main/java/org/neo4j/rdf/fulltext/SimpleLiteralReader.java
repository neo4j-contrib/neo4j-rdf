package org.neo4j.rdf.fulltext;

public class SimpleLiteralReader implements LiteralReader
{
    public String read( Object literal )
    {
        return literal.toString();
    }
}
