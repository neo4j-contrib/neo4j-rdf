/**
 * 
 */
package org.neo4j.rdf.store.testrepresentation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;

class DenseStatementRepresentation implements AbstractStatementRepresentation
{
    private final List<? extends AbstractNode> nodes;
    private final List<? extends AbstractRelationship> relationships;
    
    // Invoked when property is data property and therefore resides
    // as a Neo property on the node
    DenseStatementRepresentation( AbstractNode subjectNode )
    {            
        this.nodes = Arrays.asList( subjectNode );
        this.relationships = Collections.emptyList();
    }
    
    // Invoked when property is object property and therefore represented
    // as a relationship to another node
    DenseStatementRepresentation( AbstractNode subjectNode, String
        relationshipTypeName, AbstractNode objectNode )
    {
        this.nodes = Arrays.asList( subjectNode, objectNode );
        this.relationships = Arrays.asList(
            new AbstractRelationshipTestImpl( subjectNode, relationshipTypeName,
                objectNode ) );            
    }

    public Iterable<AbstractNode> nodes()
    {
        return Collections.unmodifiableList( nodes );
    }

    public Iterable<AbstractRelationship> relationships()
    {
        return Collections.unmodifiableList( relationships );
    }
}
