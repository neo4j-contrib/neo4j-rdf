package org.neo4j.rdf.store.testrepresentation;


import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;
import org.neo4j.rdf.store.representation.RdfRepresentationStrategy;

/**
 * S/P/O represented as:
 * if object property: ( S ) -- predicate_uri_as_reltype --> ( O )
 * if data property: ( S ) with property [key=predicate_uri, value=O]
 */
public class DenseRepresentationStrategy implements RdfRepresentationStrategy
{
    public AbstractStatementRepresentation getAbstractRepresentation( Statement
        statement )
    {
        if ( statement.getObject().isObjectProperty() )
        {
            // ( S ) -- predicate_uri --> ( O )
            return createTwoNodeFragment( statement );
        }
        else
        {
            // ( S ) with property [key=predicate_uri, value=O]
            return createOneNodeFragment( statement );
        }
    }
    
    private AbstractStatementRepresentation createTwoNodeFragment( Statement
        statement )
    {
        AbstractNodeTestImpl objectNode = new AbstractNodeTestImpl( statement.
            getObject().getResourceOrNull().uriAsString() );
        
        return new DenseStatementRepresentation( getSubjectNode( statement ),
            statement.getPredicate().uriAsString(), objectNode );
    }

    private AbstractStatementRepresentation createOneNodeFragment( Statement
        statement )
    {
        AbstractNodeTestImpl subjectNode = getSubjectNode( statement );
        subjectNode.addProperty( statement.getPredicate().uriAsString(),
            statement.getObject().getLiteralValueOrNull() );
        
        return new DenseStatementRepresentation( subjectNode );
    }

    private AbstractNodeTestImpl getSubjectNode( Statement statement )
    {
        return new AbstractNodeTestImpl( statement.getSubject().uriAsString() );
    }
}
