package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.Node;
import org.neo4j.rdf.model.Uri;

/**
 * Represents a more simple abstraction of a {@link Node}.
 */
public class AbstractNode extends AbstractElement
{
    private final String uriOrNull;
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
     * @param uriOrNull the URI of this node, or {@code null} if it's a
     * blank node.
     */
    public AbstractNode( String uriOrNull )
    {
        this.uriOrNull = uriOrNull;
    }

    /**
     * @return the {@link Uri} which this {@link AbstractNode} was constructed
     * with or {@code null} if it's a blank node.
     */
    public Uri getUriOrNull()
    {
        return uriOrNull == null ? null : new Uri( uriOrNull );
    }
}
