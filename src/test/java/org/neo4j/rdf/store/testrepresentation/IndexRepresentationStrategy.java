package org.neo4j.rdf.store.testrepresentation;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.UriMakeItSoer;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;
import org.neo4j.rdf.store.representation.MakeItSoer;
import org.neo4j.rdf.store.representation.RdfRepresentationStrategy;
import org.neo4j.util.NeoUtil;
import org.neo4j.util.index.Index;
import org.neo4j.util.index.SingleValueIndex;

public abstract class IndexRepresentationStrategy
	implements RdfRepresentationStrategy
{
	private final MakeItSoer makeItSoer;
	
	public IndexRepresentationStrategy( NeoService neo )
	{
		this.makeItSoer = new UriMakeItSoer( neo, newIndex( neo ) );
	}
	
	private static Index newIndex( NeoService neo )
	{
		Node indexNode = new NeoUtil( neo ).getOrCreateSubReferenceNode(
			MyRelTypes.INDEX_ROOT );
		return new SingleValueIndex( "blaaaa", indexNode, neo );
	}

	public MakeItSoer getMakeItSoer()
	{
		return this.makeItSoer;
	}

    protected AbstractStatementRepresentation createOneNodeFragment( Statement
        statement )
    {
        AbstractNode subjectNode = getSubjectNode( statement );
        subjectNode.addProperty( statement.getPredicate().uriAsString(),
            statement.getObject().getLiteralValueOrNull() );
        AbstractStatementRepresentation representation =
        	new AbstractStatementRepresentation();
        representation.addNode( subjectNode );
        return representation;
    }

    protected AbstractNode getSubjectNode( Statement statement )
    {
        return new AbstractNode( statement.getSubject().uriAsString() );
    }

    protected AbstractNode getObjectNode( Statement statement )
    {
        return new AbstractNode(
        	statement.getObject().getResourceOrNull().uriAsString() );
    }

    private static enum MyRelTypes implements RelationshipType
    {
    	INDEX_ROOT,
    }
}
