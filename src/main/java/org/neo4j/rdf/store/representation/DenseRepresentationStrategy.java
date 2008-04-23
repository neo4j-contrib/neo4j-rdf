package org.neo4j.rdf.store.representation;

import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;

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
        super( neo );
    }

    @Override
    protected boolean addToRepresentation(
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
	    if ( !super.addToRepresentation(
	        representation, nodeMapping, statement ) )
	    {
            if ( isObjectType( statement.getObject() ) )
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
        AbstractStatementRepresentation representation,
        Map<String, AbstractNode> nodeMapping, Statement statement )
    {
        AbstractNode subjectNode = getSubjectNode( nodeMapping, statement );
        AbstractNode objectNode = getObjectNode( nodeMapping, statement );
        AbstractRelationship relationship = new AbstractRelationship(
            subjectNode, asUri( statement.getPredicate() ), objectNode );
        for ( Context context : statement.getContexts() )
        {
            // TODO
        }
        representation.addRelationship( relationship );
    }
}
