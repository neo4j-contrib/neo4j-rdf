package org.neo4j.rdf.fulltext;

/**
 * This is a hook for the {@link FulltextIndex} where you f.ex. can return
 * the contents of the URL (if the literal represents a URL) or whatever.
 */
public interface LiteralReader
{
    /**
     * @return what is to be indexed in the fulltext index for the given
     * literal value.
     */
    String read( Object literal );
}
