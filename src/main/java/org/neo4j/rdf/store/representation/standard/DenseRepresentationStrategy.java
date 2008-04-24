package org.neo4j.rdf.store.representation.standard;

import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;

/**
 * S/P/O represented as:
 * if object property: ( S ) -- predicate_uri_as_reltype --> ( O )
 * if data property: ( S ) with property [key=predicate_uri, value=O]
 */
public class DenseRepresentationStrategy extends IndexRepresentationStrategy
{
    /**
     * @param neo the {@link NeoService}.
     */
	public DenseRepresentationStrategy( NeoService neo )
    {
        super( neo );
    }

    /**
     * @param neo the {@link NeoService}.
     * @param meta the {@link MetaStructure}.
     */
    public DenseRepresentationStrategy( NeoService neo, MetaStructure meta )
    {
        super( neo, meta );
    }

    @Override
    protected boolean addToRepresentation(
        AbstractRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
	    if ( !super.addToRepresentation(
	        representation, nodeMapping, statement ) )
	    {
            if ( isObjectType( statement.getObject() ) ||
            		( statement.getPredicate() instanceof Uri &&
            		isObjectType( ( ( Uri )
            				statement.getPredicate() ).getUriAsString() ) ) )
            {
                // ( S ) -- predicate_uri --> ( O )
                addTwoNodeFragment( representation, nodeMapping, statement );
            }
            else
            {
                // ( S ) with property [key=predicate_uri, value=O]
                addOneNodeFragment( representation, nodeMapping, statement );
            }
	    }
        return true;
    }

    private void addTwoNodeFragment(
        AbstractRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode subjectNode = getSubjectNode( nodeMapping, statement );
        AbstractNode objectNode = getObjectNode( nodeMapping, statement );
        AbstractRelationship relationship = new AbstractRelationship(
            subjectNode, asUri( statement.getPredicate() ), objectNode );
        addContextsToRelationship( statement, relationship );
        representation.addRelationship( relationship );
    }
}
