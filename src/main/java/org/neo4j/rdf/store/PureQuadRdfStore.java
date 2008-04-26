package org.neo4j.rdf.store;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.standard.DenseValidatable;
import org.neo4j.rdf.validation.Validatable;

public class PureQuadRdfStore extends RdfStoreImpl
{
    private final MetaStructure meta;

    public PureQuadRdfStore( NeoService neo, MetaStructure meta,
        WillBeQuadRepresentationStrategy representationStrategy )
    {
         super( neo, representationStrategy );
         this.meta = meta;
    }
    
    protected MetaStructure meta()
    {
        return this.meta;
    }
    
    @Override
    protected WillBeQuadRepresentationStrategy getRepresentationStrategy()
    {
        return ( WillBeQuadRepresentationStrategy ) super.getRepresentationStrategy();
    }

    @Override
    public Iterable<Statement> getStatements( WildcardStatement statement,
        boolean includeInferredStatements )
    {
        Transaction tx = neo().beginTx();
        try
        {
            if ( !includeInferredStatements )
            {
                throw new UnsupportedOperationException( "We currently only " +
                    "support getStatements() with reasoning enabled" );
            }
            
            Iterable<Statement> result = null;            
            if ( wildcardPattern( statement, false, false, true ) )
            {
                result = handleSubjectPredicateWildcard( statement );
            }
            else
            {
                result = super.getStatements( statement,
                    includeInferredStatements ); 
            }
            
            tx.success();           
            return result;
        }
        finally
        {
            tx.finish();
        }
    }
    
    private Iterable<Statement> handleSubjectPredicateWildcard(
        WildcardStatement statement )
    {
        Uri subject = ( Uri ) statement.getSubject();
        Uri predicate = ( Uri ) statement.getPredicate();
        
        AbstractNode abstractSubjectNode = new AbstractNode( subject );            
        Node subjectNode = getRepresentationStrategy().getExecutor().
            lookupNode( abstractSubjectNode );
        Validatable validatableInstance = new DenseValidatable( neo(),
            subjectNode, meta() ); 
        List<Statement> statementList = new LinkedList<Statement>();
        
        if ( getRepresentationStrategy().pointsToObjectType( predicate ) )
        {
            Collection<? extends Validatable> objectProperties =
                validatableInstance.complexProperties(
                    predicate.getUriAsString() );
            for ( Validatable objectProperty : objectProperties )
            {
                statementList.add( new CompleteStatement( subject, predicate,
                    objectProperty.getUri() ) );
            }
        }
        else
        {
            Object[] objectValues = validatableInstance.getProperties(
                predicate.getUriAsString() );
            for ( Object value : objectValues )
            {
                statementList.add( new CompleteStatement( subject, predicate,
                    new Literal( value ) ) );
            }
        }
        return statementList;
    }

    private boolean weCanHandleStatement( WildcardStatement statement )
    {
        return 
            wildcardPattern( statement, false, false, true ) ||
            wildcardPattern( statement, false, true, true ) ||
            wildcardPattern( statement, true, true, false ) ||
            wildcardPattern( statement, true, false, false );
    }
}
