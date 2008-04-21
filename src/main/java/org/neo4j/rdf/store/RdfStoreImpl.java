package org.neo4j.rdf.store;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.representation.AbstractElement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.RdfRepresentationStrategy;

public class RdfStoreImpl implements RdfStore
{
    private static final String URI_PROPERTY_KEY = "uri";
    private final NeoService neo;
    private final RdfRepresentationStrategy representationStrategy;
    private final UriLookupService uriLookupService;
    
    public RdfStoreImpl( NeoService neo, RdfRepresentationStrategy
        representationStrategy, UriLookupService uriLookupService ) 
    {
        this.neo = neo;
        this.representationStrategy = representationStrategy;
        this.uriLookupService = uriLookupService;
    }
    
    public void addStatement( Statement statement, Context... contexts )
    {
        Transaction tx = neo.beginTx();
        try
        {
             AbstractStatementRepresentation fragment = representationStrategy.
                 getAbstractRepresentation( statement );
             
             for ( AbstractNode node : fragment.nodes() )
             {
                 writeNode( node );
             }
             
             for ( AbstractRelationship rel : fragment.relationships() )
             {
                 writeRelationship( rel );
             }             
             tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    // assumes tx
    private void writeNode( AbstractNode abstractNode )
    {
        // Get underlying node by URI
        Node underlyingNode = uriLookupService.getNodeForUri(
            abstractNode.getUriOrNull() );
        
        // Create new node if none found
        if ( underlyingNode == null )
        {
            underlyingNode = neo.createNode();
            underlyingNode.setProperty( URI_PROPERTY_KEY,
                abstractNode.getUriOrNull().uriAsString() );
        }
        
        // Attach properties
        for ( Map.Entry<String, Object> property :
            abstractNode.properties().entrySet() )
        {
            underlyingNode.setProperty( property.getKey(),
                property.getValue() );
        }
    }

    // assumes tx
    private void writeRelationship( final AbstractRelationship rel )
    {
        // Both nodes must exist at this point since they were just lookup up
        // or created in writeNodes()
        Node startNode = uriLookupService.getNodeForUri(
            rel.getStartNode().getUriOrNull() );
        Node endNode = uriLookupService.getNodeForUri(
            rel.getEndNode().getUriOrNull() );
        // Probably really need to verify duplicates and stuff before we create
        startNode.createRelationshipTo( endNode, new RelationshipType()
        {
            public String name()
            {
                return rel.getRelationshipTypeName();
            }
        } );        
    }

    public Iterable<Statement> getStatements(
        Statement statementWithOptionalNulls,
        boolean includeInferredStatements, Context... contexts )
    {
        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    public void removeStatements( Statement statementWithOptionalNulls,
        Context... contexts )
    {
        throw new UnsupportedOperationException( "Not yet implemented" );
    }
    
    // Ignore wildcards and named graphs => no null in statement, no contexts
    Iterable<? extends Statement> removeStatementsSimple( Statement statement,
        boolean includeInferredStatements )
    {
        // Example: <http://eifrem.com/emil> dc:author <http://.../article.html>
        assert !includeInferredStatements;
                
        AbstractStatementRepresentation fragment = representationStrategy.
            getAbstractRepresentation( statement );
        
        Map<AbstractElement, NodeOrRelationship> mapping =
            resolveFragment( fragment );

        for ( NodeOrRelationship primitive : mapping.values() )
        {
            if ( primitive.node != null )
            {
                primitive.node.delete();
            }
            else
            {
                primitive.relationship.delete();
            }
        }
        
        return null;
    }
    
    private Map<AbstractElement, NodeOrRelationship> resolveFragment(
        AbstractStatementRepresentation fragment )
    {
        Map<AbstractElement, NodeOrRelationship> mapping =
            new HashMap<AbstractElement, NodeOrRelationship>();
        
        for ( AbstractNode abstractNode : fragment.nodes() )
        {
            // Get underlying node by URI
            Node underlyingNode = uriLookupService.getNodeForUri(
                abstractNode.getUriOrNull() );
            mapping.put( abstractNode, underlyingNode != null
                ? new NodeOrRelationship( underlyingNode )
                : NodeOrRelationship.NOT_IN_NODE_SPACE ); 
        }
        
        // TODO map relationships as well
        return mapping;
    }

    private static class NodeOrRelationship
    {
        static final NodeOrRelationship NOT_IN_NODE_SPACE =
            new NodeOrRelationship( ( Node ) null ); 
        private Node node = null;
        private Relationship relationship = null;
        NodeOrRelationship( Node node )
        {
            this.node = node;
        }
        NodeOrRelationship( Relationship relationship )
        {
            this.relationship = relationship;
        }        
    }
}
