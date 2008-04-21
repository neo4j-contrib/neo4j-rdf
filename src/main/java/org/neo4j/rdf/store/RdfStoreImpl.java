package org.neo4j.rdf.store;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Transaction;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.store.representation.AbstractStatementRepresentation;
import org.neo4j.rdf.store.representation.AsrExecutor;
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
             getAsrExecutor().addToNodeSpace( fragment );
             tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
    
    private AsrExecutor getAsrExecutor()
    {
    	return this.representationStrategy.getAsrExecutor();
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
             getAsrExecutor().removeFromNodeSpace( fragment );
             tx.success();
        }
        finally
        {
            tx.finish();
        }
    }    
}
