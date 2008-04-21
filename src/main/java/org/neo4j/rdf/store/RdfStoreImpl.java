package org.neo4j.rdf.store;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Transaction;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.representation.AbstractElement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;
import org.neo4j.rdf.store.representation.MakeItSoer;
import org.neo4j.rdf.store.representation.RdfRepresentationStrategy;

public class RdfStoreImpl implements RdfStore
{
    private final NeoService neo;
    private final RdfRepresentationStrategy representationStrategy;
    
    public RdfStoreImpl( NeoService neo,
    	RdfRepresentationStrategy representationStrategy ) 
    {
        this.neo = neo;
        this.representationStrategy = representationStrategy;
    }
    
    public void addStatement( Statement statement, Context... contexts )
    {
    	System.out.println( "--- addStatement( " + statement.getSubject() +
    		", " + statement.getPredicate() + ", " + statement.getObject() );
        Transaction tx = neo.beginTx();
        try
        {
             AbstractStatementRepresentation fragment = representationStrategy.
                 getAbstractRepresentation( statement );
             getMakeItSoer().apply( fragment );
             tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
    
    private MakeItSoer getMakeItSoer()
    {
    	return this.representationStrategy.getMakeItSoer();
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
    	if ( statementWithOptionalNulls.getSubject() == null ||
    		statementWithOptionalNulls.getPredicate() == null ||
    		statementWithOptionalNulls.getObject() == null )
    	{
            throw new UnsupportedOperationException( "Not yet implemented" );
    	}
    	removeStatementsSimple( statementWithOptionalNulls );
    }
    
    private void removeStatementsSimple( Statement statement )
    {
    	System.out.println( "--- removeStatement( " + statement.getSubject() +
    		", " + statement.getPredicate() + ", " + statement.getObject() );
        Transaction tx = neo.beginTx();
        try
        {
             AbstractStatementRepresentation fragment = representationStrategy.
                 getAbstractRepresentation( statement );
             getMakeItSoer().remove( fragment );
             tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
    
    // Ignore wildcards and named graphs => no null in statement, no contexts
//    Iterable<? extends Statement> removeStatementsSimple( Statement statement,
//        boolean includeInferredStatements )
//    {
//        // Example: <http://eifrem.com/emil> dc:author <http://.../article.html>
//        assert !includeInferredStatements;
//                
//        AbstractStatementRepresentation fragment = representationStrategy.
//            getAbstractRepresentation( statement );
//        
//        Map<AbstractElement, NodeOrRelationship> mapping =
//            resolveFragment( fragment );
//
//        for ( NodeOrRelationship primitive : mapping.values() )
//        {
//            if ( primitive.relationship != null )
//            {
//                primitive.relationship.delete();
//            }
//        }
//        for ( NodeOrRelationship primitive : mapping.values() )
//        {
//            if ( primitive.node != null )
//            {
//                primitive.node.delete();
//            }
//        }
//        
//        return null;
//    }
    
    private Map<AbstractElement, NodeOrRelationship> resolveFragment(
        AbstractStatementRepresentation fragment )
    {
        Map<AbstractElement, NodeOrRelationship> mapping =
            new HashMap<AbstractElement, NodeOrRelationship>();
        
        for ( AbstractNode abstractNode : fragment.nodes() )
        {
            Node underlyingNode = getMakeItSoer().lookupNode( abstractNode );
            mapping.put( abstractNode, underlyingNode != null
                ? new NodeOrRelationship( abstractNode, underlyingNode )
                : NodeOrRelationship.NOT_IN_NODE_SPACE ); 
        }
        
        for ( AbstractRelationship abstractRelationship :
        	fragment.relationships() )
        {
        	Relationship underlyingRelationship =
        		getMakeItSoer().lookupRelationship( abstractRelationship );
            mapping.put( abstractRelationship, underlyingRelationship != null
                ? new NodeOrRelationship( abstractRelationship,
                	underlyingRelationship )
                : NodeOrRelationship.NOT_IN_NODE_SPACE ); 
        }
        return mapping;
    }

    private static class NodeOrRelationship
    {
        static final NodeOrRelationship NOT_IN_NODE_SPACE =
            new NodeOrRelationship( null, ( Node ) null ); 
        private Node node;
        private Relationship relationship;
        private AbstractElement abstractElement;
        
        NodeOrRelationship( AbstractNode abstractNode, Node node )
        {
        	this.abstractElement = abstractNode;
            this.node = node;
        }
        NodeOrRelationship( AbstractRelationship abstractRelationship,
        	Relationship relationship )
        {
        	this.abstractElement = abstractRelationship;
            this.relationship = relationship;
        }        
    }
}
