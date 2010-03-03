package org.neo4j.rdf.store;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.fulltext.QueryResult;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationExecutor;
import org.neo4j.rdf.store.representation.RepresentationStrategy;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;

/**
 * Default implementation of an {@link RdfStore}.
 */
public abstract class RdfStoreImpl implements RdfStore
{
    private final GraphDatabaseService graphDb;
    private final RepresentationStrategy representationStrategy;
    private boolean shutdownNeo4jInstancesUponShutdown;

    /**
     * @param graphDb the {@link GraphDatabaseService}.
     * @param representationStrategy the {@link RepresentationStrategy}
     * to use when storing statements.
     */
    public RdfStoreImpl( GraphDatabaseService graphDb,
        RepresentationStrategy representationStrategy )
    {
        this.graphDb = graphDb;
        this.representationStrategy = representationStrategy;
    }

    /**
     * @param shutdownNeo4jInstances If {@code true} then the call to
     * {@link #shutDown()} will also shutdown.
     */
    public void setShutdownNeo4jInstancesUponShutdown( boolean shutdownNeo4jInstances )
    {
        this.shutdownNeo4jInstancesUponShutdown = shutdownNeo4jInstances;
    }
    
    protected GraphDatabaseService graphDb()
    {
        return this.graphDb;
    }

    public RepresentationStrategy getRepresentationStrategy()
    {
        return this.representationStrategy;
    }

    public void addStatements( CompleteStatement... statements )
    {
        Transaction tx = graphDb.beginTx();
        try
        {
            for ( Statement statement : statements )
            {
                addStatement( statement );
            }
            tx.success();
        }
        catch ( RuntimeException e )
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            tx.finish();
        }
    }

    protected void addStatement( Statement statement )
    {
        AbstractRepresentation fragment = representationStrategy
            .getAbstractRepresentation( statement,
                new AbstractRepresentation() );
        getExecutor().addToNodeSpace( fragment );
    }

    private RepresentationExecutor getExecutor()
    {
        return this.representationStrategy.getExecutor();
    }


    protected Node lookupNode( Value uri )
    {
        return getRepresentationStrategy().getExecutor().lookupNode(
            new AbstractNode( uri ) );
    }
    
    protected RelationshipType relType( final String name )
    {
        return new RelationshipType()
        {
            public String name()
            {
                return name;
            }
        };
    }
    
    protected RelationshipType relType( Value value )
    {
        return relType( ( ( Uri ) value ).getUriAsString() );
    }
    
    protected RelationshipType relType( Statement statement )
    {
        return relType( statement.getPredicate() );
    }
    
    public Iterable<CompleteStatement> getStatements(
        WildcardStatement statement, boolean includeInferredStatements )
    {
//        if ( weCanHandleStatement( statement ) )
//        {
//            // TODO: pseudo code below
//            return graphMatchingFacade().getMatchingStatements(
//                representationStrategy.getAbstractRepresentation( statement ) );
//        }
        throw new UnsupportedOperationException( "We can't handle get() for " +
            "this statement: " + statement );
    }

//    private boolean weCanHandleStatement( WildcardStatement statement )
//    {
//        return false;
//    }
    
    public Iterable<QueryResult> searchFulltext( String query )
    {
    	throw new UnsupportedOperationException( "No implementation here" );
    }
    
    public Iterable<QueryResult> searchFulltextWithSnippets( String query,
        int snippetCountLimit )
    {
        throw new UnsupportedOperationException( "No implementation here" );
    }
    
    public int size( Context... contexts )
    {
    	throw new UnsupportedOperationException();
    }

    protected boolean wildcardPattern( WildcardStatement statement,
        boolean subjectWildcard, boolean predicateWildcard,
        boolean objectWildcard )
    {
        return valueIsWildcard( statement.getSubject() ) == subjectWildcard &&
            valueIsWildcard( statement.getPredicate() ) == predicateWildcard &&
            valueIsWildcard( statement.getObject() ) == objectWildcard;
    }

    private boolean valueIsWildcard( Value potentialWildcard )
    {
        return potentialWildcard instanceof Wildcard;
    }

//    public Iterable<Statement> oldGetStatements( WildcardStatement statement,
//        boolean includeInferredStatements )
//    {
//      S, null, null         : No
//      S, P, null            : Yes
//      null, null, O         : No
//      null, P, O            : Yes (for objecttype)

//      if ( theseAreNull( statementWithOptionalNulls, false, true, true ) )
//      {
//
//      }
//      else if ( theseAreNull( statementWithOptionalNulls,
//          false, false, true ) )
//      {
//
//      }
//      else if ( theseAreNull( statementWithOptionalNulls,
//          true, true, false ) )
//      {
//      }
//      else if ( theseAreNull( statementWithOptionalNulls,
//          true, false, false ) )
//      {
//
//      }
//      String query = SparqlBuilder.getQuery( statementWithOptionalNulls );
//
//        throw new UnsupportedOperationException();
//    }


//    private boolean theseAreNull( Statement statementWithOptionalNulls,
//        boolean subjectIsNull, boolean predicateIsNull, boolean objectIsNull )
//    {
//        return objectComparesToNull( statementWithOptionalNulls.getSubject(),
//            subjectIsNull )
//            && objectComparesToNull( statementWithOptionalNulls.getPredicate(),
//                predicateIsNull )
//            && objectComparesToNull( statementWithOptionalNulls.getObject(),
//                objectIsNull );
//    }

//    private boolean objectComparesToNull( Object object, boolean shouldBeNull )
//    {
//        return shouldBeNull ? object == null : object != null;
//    }

    public void removeStatements( WildcardStatement statement )
    {
        for ( Statement statementFromGet : getStatements( statement, false ) )
        {
            removeStatementSimple( statementFromGet );
        }
    }

    private void removeStatementSimple( Statement statement )
    {
        Transaction tx = graphDb.beginTx();
        try
        {
            AbstractRepresentation fragment = representationStrategy
                .getAbstractRepresentation( statement,
                    new AbstractRepresentation() );
            getExecutor().removeFromNodeSpace( fragment );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
    
    public void shutDown()
    {
//        TemporaryLogger.getLogger().info( getClass().getName() +
//            " shutDown called", new Exception( "Just a stack trace to see " +
//            "where the shut down method is called" ) );
        
        FulltextIndex index = getFulltextIndex();
        if ( index != null )
        {
            index.shutDown();
        }
        
        // Shut down the Neo4j stuff if that flag is set.
        if ( this.shutdownNeo4jInstancesUponShutdown )
        {
            // TODO This isn't a very nice check (instanceof)
            RepresentationExecutor executor = this.getExecutor();
            if ( executor instanceof AbstractUriBasedExecutor )
            {
                ( (AbstractUriBasedExecutor) executor ).index().shutdown();
            }
            graphDb.shutdown();
        }
    }
    
    /**
     * @return the {@link FulltextIndex} instance on the executor, or
     * <code>null</code> if no fulltext index is used.
     */
    public FulltextIndex getFulltextIndex()
    {
        return ( ( AbstractUriBasedExecutor )
            getRepresentationStrategy().getExecutor() ).getFulltextIndex();
    }
    
    /**
     * Performs a reindex of the entire fulltext index. Please call
     * {@link FulltextIndex#clear()} before using this.
     */
    public void reindexFulltextIndex()
    {
        throw new UnsupportedOperationException();
    }
    
    protected static class InferenceOptions
    {
        private boolean includeSubClassOf;
        private boolean includeSubPropertyOf;
        
        protected InferenceOptions( boolean includeSubClassOf,
            boolean includeSubPropertyOf )
        {
            this.includeSubClassOf = includeSubClassOf;
            this.includeSubPropertyOf = includeSubPropertyOf;
        }
        
        public boolean isIncludeSubClassOf()
        {
            return includeSubClassOf;
        }
        
        public boolean isIncludeSubPropertyOf()
        {
            return includeSubPropertyOf;
        }
    }

//    private static interface GraphMatchingFacade
//    {
//        /**
//         * Takes an abstract representation of a <i>single</i> statement with
//         * optional wildcards, and returns all matching statements in the
//         * node space.
//         * @param oneStatementWithWildcards an abstract representaiton of a
//         * single statement, potentially with wildcards
//         * @return all statements that match
//         * <code>oneStatementWithWildcards</code>
//         */
//        Iterable<Statement> getMatchingStatements( AbstractRepresentation
//            oneStatementWithWildcards );
//    }

/*    private GraphMatchingFacade graphMatchingFacade()
    {
        return new GraphMatchingFacade()
        {
            public Iterable<Statement> getMatchingStatements(
                AbstractRepresentation statementRepresentation )
            {
                Map<PatternElement, AbstractElement>
                    patternToRepresentationMap =
                        buildPatternGraphFromAbstractRepresentation(
                            statementRepresentation );

                Iterable<PatternMatch> matches = runMatchingEngine(
                    figureOutPatternStartNode( patternToRepresentationMap ),
                    figureOutGraphDbStartNode( statementRepresentation ) );

                for ( PatternMatch match : matches )
                {
                    // get abstract element for every pattern element
                    // get value for every abstract element
                    // construct statements
                }
                return null;
            }

            private Map<PatternElement, AbstractElement>
                buildPatternGraphFromAbstractRepresentation(
                    AbstractRepresentation statementRepresentation )
            {
                return null;
            }

            private PatternNode figureOutPatternStartNode(
                Map<PatternElement, AbstractElement> map )
            {
                return null;
            }

            private Iterable<PatternMatch> runMatchingEngine(
                PatternNode patternStartNode, Node graphDbStartNode )
            {
                return PatternMatcher.getMatcher().match( patternStartNode,
                    graphDbStartNode );
            }

            private Node figureOutGraphDbStartNode( AbstractRepresentation
                statementRepresentation )
            {
                // Resolve abstract nodes in representation to nodes
                // through the executor
                // Investigate meta model, check instance counts etc
                // Return a smart start node
                return null;
            }
        };
    }*/
}
