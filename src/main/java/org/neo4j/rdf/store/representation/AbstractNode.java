package org.neo4j.rdf.store.representation;

import org.neo4j.graphdb.Node;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;

/**
 * Represents a more simple abstraction of a {@link Node}.
 */
public class AbstractNode extends AbstractElement
{
    private final Value wildcardOrUriOrNull;
    private final Object keyOrNull;

    /**
     * @param wildcardOrUriOrNull the URI of this node, a wildcard, or {@code null} if
     * it's a blank node.
     */
    public AbstractNode( Value wildcardOrUriOrNull )
    {
        this( wildcardOrUriOrNull, null );
    }
    
    public AbstractNode( Value wildcardOrUriOrNull,
        Object alternativeKeyOrNull )
    {
        this.wildcardOrUriOrNull = wildcardOrUriOrNull;
        this.keyOrNull = alternativeKeyOrNull;
    }
    
    public Object getKey()
    {
        return this.keyOrNull != null ?
            this.keyOrNull : this.wildcardOrUriOrNull;
    }

    /**
     * @return the {@link Uri} which this {@link AbstractNode} was constructed
     * with or {@code null} if it's a wildcard or blank node.
     */
    public Uri getUriOrNull()
    {
        return this.wildcardOrUriOrNull == null ||
            !( this.wildcardOrUriOrNull instanceof Uri )
            ? null : ( Uri ) this.wildcardOrUriOrNull;
    }

    /**
     * @return the {@link Wildcard} which this {@link AbstractNode} was
     * constructed with or {@code null} if it's a {@link Uri} or a blank node.
     */
    public Wildcard getWildcardOrNull()
    {
        return this.wildcardOrUriOrNull == null ||
            !( this.wildcardOrUriOrNull instanceof Wildcard ) ?
                null : ( Wildcard ) this.wildcardOrUriOrNull;
    }
    
    /**
     * @return true if this {@link AbstractNode} is a wildcard.
     */
    public boolean isWildcard()
    {
    	return this.wildcardOrUriOrNull instanceof Wildcard;
    }
    
    @Override
    public String toString()
    {
        return "AbstractNode[value=" + this.wildcardOrUriOrNull + ", " +
            "key=" + this.keyOrNull + "]";
    }
}
