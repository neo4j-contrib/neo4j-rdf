package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

public interface MakeItSoer
{
	void apply( AbstractStatementRepresentation representation );
	
	Node lookupNode( AbstractNode node );
	
	Relationship lookupRelationship( AbstractRelationship relationship );
}
