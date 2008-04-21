/**
 * 
 */
package org.neo4j.rdf.store.testrepresentation;

import java.util.Collections;
import java.util.Map;

import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;

class AbstractRelationshipTestImpl implements
    AbstractRelationship
{
    private final String RELATIONSHIP_TYPE_NAME;
    private final AbstractNode startNode, endNode;
    AbstractRelationshipTestImpl( AbstractNode startNode, String relTypeName,
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
    
    public AbstractNode[] getBothNodes()
    {
        return new AbstractNode[] { startNode, endNode };
    }
    
    public Map<String, Object> properties()
    {
        return Collections.emptyMap();
    }        
}