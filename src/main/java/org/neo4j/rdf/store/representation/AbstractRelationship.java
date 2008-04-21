package org.neo4j.rdf.store.representation;

import java.util.Map;

public interface AbstractRelationship extends AbstractElement
{
    String getRelationshipTypeName();
    AbstractNode getStartNode();
    AbstractNode getEndNode();
    AbstractNode[] getBothNodes();
    Map<String, Object> properties();    
}
