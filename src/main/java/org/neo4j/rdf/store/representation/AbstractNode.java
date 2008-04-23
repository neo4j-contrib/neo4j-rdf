package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.Node;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;

/**
 * Represents a more simple abstraction of a {@link Node}.
 */
public class AbstractNode extends AbstractElement
{
    private final Value valueOrNull;
    private NodeMatcher nodeMatcher;
    
    public void setOptionalMatcher( NodeMatcher matcher )
    {
        this.nodeMatcher = matcher;
    }
    
    public NodeMatcher getOptionalMatcher()
    {
        return this.nodeMatcher;
    }

    /**
     * @param valueOrNull the URI of this node, a wildcard, or {@code null} if
     * it's a blank node.
     */
    public AbstractNode( Value valueOrNull )
    {
    	this.valueOrNull = valueOrNull;
    }

    /**
     * @return the {@link Uri} which this {@link AbstractNode} was constructed
     * with or {@code null} if it's a wildcard or blank node.
     */
    public Uri getUriOrNull()
    {
        return this.valueOrNull == null || !( this.valueOrNull instanceof Uri )
        ? null : ( Uri ) this.valueOrNull;
    }

    /**
     * @return the {@link Wildcard} which this {@link AbstractNode} was
     * constructed with or {@code null} if it's a {@link Uri} or a blank node.
     */
    public Wildcard getWildcardOrNull()
    {
    	return this.valueOrNull == null ||
    		!( this.valueOrNull instanceof Wildcard ) ?
    			null : ( Wildcard ) this.valueOrNull;
    }
    
    /**
     * @return true if this {@link AbstractNode} is a wildcard.
     */
    public boolean isWildcard()
    {
    	return this.valueOrNull instanceof Wildcard;
    }
}
