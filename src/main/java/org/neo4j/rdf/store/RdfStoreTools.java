package org.neo4j.rdf.store;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseQuadExecutor;

public class RdfStoreTools
{
    private final RdfStore rdfStore;

    public RdfStoreTools( RdfStore rdfStore )
    {
        this.rdfStore = rdfStore;
    }
    
    /**
     * Renames a context URI to a new value. It will reflect all statements
     * in that context, but the operation is instant, no matter how many
     * statements it contains.
     * 
     * NOTE: It only works for {@link VerboseQuadStore} at the moment.
     * 
     * @param context the {@link Context} to rename.
     * @param newValue the new value of that context.
     */
    public void renameContext( Context context, Context newValue )
    {
        if ( !( rdfStore instanceof VerboseQuadStore ) )
        {
            throw new RuntimeException( "This operation only works on " +
                    VerboseQuadStore.class.getName() );
        }
        
        VerboseQuadStore store = (VerboseQuadStore) rdfStore;
        Transaction tx = store.graphDb().beginTx();
        try
        {
            String key = AbstractUriBasedExecutor.URI_PROPERTY_KEY;
            Node contextNode = ((VerboseQuadStore)
                    rdfStore).getRepresentationStrategy().getExecutor().lookupNode(
                            new AbstractNode( context ) );
            ensureIsContextNode( contextNode, context );
            String uri = getContextNodeUri( contextNode );
            String newUri = newValue.getUriAsString();
            contextNode.setProperty( key, newUri );
            store.getRepresentationStrategy().getExecutor().index().removeIndex(
                    contextNode, key, uri );
            store.getRepresentationStrategy().getExecutor().index().index(
                    contextNode, key, newUri );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private void ensureIsContextNode( Node contextNode, Context context )
    {
        String nodeContextUri = getContextNodeUri( contextNode );
        if ( !context.getUriAsString().equals( nodeContextUri ) )
        {
            throw new RuntimeException( "Context node " + contextNode +
                    " isn't what it seems to be, " + nodeContextUri );
        }
        
        if ( !contextNode.hasRelationship( VerboseQuadExecutor.RelTypes.IS_A_CONTEXT,
                Direction.INCOMING ) )
        {
            throw new RuntimeException( contextNode + " (" + context +
                    ") is not a context node" );
        }
    }

    private String getContextNodeUri( Node contextNode )
    {
        String nodeContextUri = (String) contextNode.getProperty(
                AbstractUriBasedExecutor.URI_PROPERTY_KEY );
        return nodeContextUri;
    }
}
