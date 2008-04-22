package org.neo4j.rdf.store.representation;

import java.util.Collections;
import java.util.Map;

public class AbstractRelationship extends AbstractElement
{
    private final String RELATIONSHIP_TYPE_NAME;
    private final AbstractNode startNode, endNode;

    public AbstractRelationship( AbstractNode startNode, String relTypeName,
        AbstractNode endNode )
    {
        this.RELATIONSHIP_TYPE_NAME = relTypeName;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public String getRelationshipTypeName()
    {
        return RELATIONSHIP_TYPE_NAME;
    }

    public AbstractNode getStartNode()
    {
        return startNode;
    }

    public AbstractNode getEndNode()
    {
        return endNode;
    }

    public AbstractNode getOtherNode( AbstractNode oneNode )
    {
        if ( oneNode != startNode && oneNode != endNode )
        {
            throw new IllegalArgumentException( "Neither start nor end node" );
        }
        return oneNode == startNode ? endNode : startNode;
    }

    public AbstractNode[] getBothNodes()
    {
        return new AbstractNode[] { startNode, endNode };
    }

    public Map<String, Object> properties()
    {
        return Collections.emptyMap();
    }
}
