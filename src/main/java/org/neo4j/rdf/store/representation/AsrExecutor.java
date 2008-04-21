package org.neo4j.rdf.store.representation;

public interface AsrExecutor
{
	void addToNodeSpace( AbstractStatementRepresentation representation );
	
	void removeFromNodeSpace( AbstractStatementRepresentation representation );	
}
