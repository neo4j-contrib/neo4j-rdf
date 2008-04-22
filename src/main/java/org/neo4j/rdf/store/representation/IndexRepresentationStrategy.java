package org.neo4j.rdf.store.representation;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.UriAsrExecutor;
import org.neo4j.util.NeoUtil;
import org.neo4j.util.index.Index;
import org.neo4j.util.index.SingleValueIndex;

/**
 * Abstract class which holds common functionality for
 * {@link RdfRepresentationStrategy} implementations using an
 * {@link UriAsrExecutor}.
 */
abstract class IndexRepresentationStrategy implements
    RdfRepresentationStrategy
{
    private final AsrExecutor executor;

    /**
     * @param neo the {@link NeoService}.
     */
    public IndexRepresentationStrategy( NeoService neo )
    {
        this.executor = new UriAsrExecutor( neo, newIndex( neo ) );
    }

    private static Index newIndex( NeoService neo )
    {
        Node indexNode = new NeoUtil( neo )
            .getOrCreateSubReferenceNode( MyRelTypes.INDEX_ROOT );
        return new SingleValueIndex( "blaaaa", indexNode, neo );
    }

    public AsrExecutor getAsrExecutor()
    {
        return this.executor;
    }

    protected AbstractStatementRepresentation createOneNodeFragment(
        Statement statement )
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
        return new AbstractNode( statement.getObject().getResourceOrNull()
            .uriAsString() );
    }

    private static enum MyRelTypes implements RelationshipType
    {
        /**
         * Neo reference node --> Uri index node.
         */
        INDEX_ROOT,
    }
}
