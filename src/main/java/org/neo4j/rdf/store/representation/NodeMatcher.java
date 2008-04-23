package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.Node;

public interface NodeMatcher
{
    boolean matches( Node node );
}
