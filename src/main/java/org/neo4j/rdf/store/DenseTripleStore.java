package org.neo4j.rdf.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.FilteringIterator;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.helpers.collection.NestingIterator;
import org.neo4j.helpers.collection.PrefetchingIterator;
import org.neo4j.index.IndexService;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;
import org.neo4j.rdf.store.representation.standard.DenseTripleStrategy;
import org.neo4j.rdf.store.representation.standard.UriBasedExecutor;
import org.neo4j.util.GraphDatabaseUtil;

public class DenseTripleStore extends RdfStoreImpl
{
    private final MetaModel model;
    private final GraphDatabaseUtil graphDbUtil;
    
    public DenseTripleStore( GraphDatabaseService graphDb, IndexService indexer )
    {
        this( graphDb, indexer, null, null );
    }
    
    public DenseTripleStore( GraphDatabaseService graphDb, IndexService indexer,
        MetaModel model, FulltextIndex fulltextIndex )
    {
        super( graphDb, new DenseTripleStrategy( new UriBasedExecutor( graphDb,
            indexer, model, fulltextIndex ), model ) );
        this.model = model;
        this.graphDbUtil = new GraphDatabaseUtil( graphDb );
    }

    @Override
    public DenseTripleStrategy getRepresentationStrategy()
    {
        return ( DenseTripleStrategy ) super.getRepresentationStrategy();
    }
    
    @Override
    public Iterable<CompleteStatement> getStatements(
        WildcardStatement statement, boolean includeInferredStatements )
    {
        Transaction tx = graphDb().beginTx();
        try
        {
            if ( includeInferredStatements )
            {
                throw new UnsupportedOperationException( "We currently not " +
                "support getStatements() with reasoning enabled" );
            }
            
            Iterable<CompleteStatement> result = null;
            if ( wildcardPattern( statement, false, false, true ) )
            {
                result = handleSubjectPredicateWildcard( statement );
            }
            else if ( wildcardPattern( statement, false, true, true ) )
            {
                result = handleSubjectWildcardWildcard( statement );
            }
            else if ( wildcardPattern( statement, false, true, false ) )
            {
                result = handleSubjectWildcardObject( statement );
            }
            else if ( wildcardPattern( statement, true, true, false ) )
            {
                result = handleWildcardWildcardObject( statement );
            }
            else if ( wildcardPattern( statement, true, false, false ) )
            {
                result = handleWildcardPredicateObject( statement );
            }
            else if ( wildcardPattern( statement, false, false, false ) )
            {
                result = handleSubjectPredicateObject( statement );
            }
            else if ( wildcardPattern( statement, true, false, true ) )
            {
//                result = handleWildcardPredicateWildcard( statement );
            }
            else if ( wildcardPattern( statement, true, true, true ) )
            {
//                result = handleWildcardWildcardWildcard( statement );
            }
            else
            {
                result = super.getStatements( statement,
                    includeInferredStatements );
            }
            
            if ( result == null )
            {
                result = new LinkedList<CompleteStatement>();
            }
            
            tx.success();
            return result;
        }
        finally
        {
            tx.finish();
        }
    }
    
    public boolean verifyFulltextIndex( String queryOrNullForAll )
    {
        throw new UnsupportedOperationException( "Not implemented yet" );
    }
    
    private Iterator<Node> getNodesForLiteral( Statement statement )
    {
        Literal literal = ( Literal ) statement.getObject();
        Iterable<Node> literalNodes = getRepresentationStrategy().
            getExecutor().findLiteralNodes( literal.getValue() );
        return literalNodes.iterator();
    }

    private Iterable<CompleteStatement> handleSubjectPredicateWildcard(
        WildcardStatement statement )
    {
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }
        
        Iterator<Object[]> triples = new ResourceToTripleIterator( subjectNode,
            ( ( Uri ) statement.getPredicate() ).getUriAsString(),
            Direction.OUTGOING );
        return statementIterator( triples );
    }
    
    private Iterable<CompleteStatement> handleSubjectWildcardWildcard(
        WildcardStatement statement )
    {
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }
        
        Iterator<Object[]> triples = new ResourceToTripleIterator( subjectNode,
            null, Direction.OUTGOING );
        return statementIterator( triples );
    }
    
    private Iterable<CompleteStatement> handleSubjectWildcardObject(
        WildcardStatement statement )
    {
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }

        Iterator<Object[]> triples = new ResourceToTripleIterator( subjectNode,
            null, Direction.OUTGOING );
        triples = new ObjectFilteredIterator( triples, statement.getObject() );
        return statementIterator( triples );
    }
    
    private Iterable<CompleteStatement> handleWildcardWildcardObject(
        WildcardStatement statement )
    {
        Iterator<Object[]> triples = null;
        if ( statement.getObject() instanceof Literal )
        {
            triples = new LiteralsToTriplesIterator(
                getNodesForLiteral( statement ), null );
            triples = new ObjectFilteredIterator( triples,
                statement.getObject() );
        }
        else
        {
            Node objectNode = lookupNode( statement.getObject() );
            if ( objectNode == null )
            {
                return null;
            }
    
            triples = new ResourceToTripleIterator( objectNode,
                null, Direction.INCOMING );
        }
        return statementIterator( triples );
    }
    
    private Iterable<CompleteStatement> handleWildcardPredicateObject(
        WildcardStatement statement )
    {
        Iterator<Object[]> triples = null;
        if ( statement.getObject() instanceof Literal )
        {
            String predicate =
                ( ( Uri ) statement.getPredicate() ).getUriAsString();
            triples = new LiteralsToTriplesIterator(
                getNodesForLiteral( statement ), predicate );
            triples = new ObjectFilteredIterator( triples,
                statement.getObject() );
        }
        else
        {
            Node objectNode = lookupNode( statement.getObject() );
            if ( objectNode == null )
            {
                return null;
            }
    
            triples = new ResourceToTripleIterator(
                objectNode,
                ( ( Uri ) statement.getPredicate() ).getUriAsString(),
                Direction.INCOMING );
        }
        return statementIterator( triples );
   }
    
    private Iterable<CompleteStatement> handleSubjectPredicateObject(
        WildcardStatement statement )
    {
        Node subjectNode = lookupNode( statement.getSubject() );
        if ( subjectNode == null )
        {
            return null;
        }
        
        Iterator<Object[]> triples = new ResourceToTripleIterator( subjectNode,
            ( ( Uri ) statement.getPredicate() ).getUriAsString(),
            Direction.OUTGOING );
        triples = new ObjectFilteredIterator( triples, statement.getObject() );
        return statementIterator( triples );
    }
    
    private Iterable<CompleteStatement> statementIterator(
        final Iterator<Object[]> triples )
    {
        return new Iterable<CompleteStatement>()
        {
            public Iterator<CompleteStatement> iterator()
            {
                return new TripleToStatementIterator( triples );
            }
        };
    }
    
    private String getNodeUriOrNull( Node node )
    {
        return ( String ) node.getProperty(
            AbstractUriBasedExecutor.URI_PROPERTY_KEY, null );
    }
    
    private Value getValueForObject( Object objectFromTriple )
    {
        return objectFromTriple instanceof Node ?
            new Uri( getNodeUriOrNull( ( Node ) objectFromTriple ) ) :
            new Literal( objectFromTriple );
    }
    
    private class TripleToStatementIterator
        extends IteratorWrapper<CompleteStatement, Object[]>
    {
        private TripleToStatementIterator( Iterator<Object[]> source )
        {
            super( source );
        }

        @Override
        protected CompleteStatement underlyingObjectToObject( Object[] triple )
        {
            Node subjectNode = ( Node ) triple[ 0 ];
            Resource subject = new Uri( getNodeUriOrNull( subjectNode ) );
            Uri predicate = new Uri( ( String ) triple[ 1 ] );
            Value object = getValueForObject( triple[ 2 ] );
            return object instanceof Resource ?
                new CompleteStatement( subject, predicate, ( Resource ) object,
                    Context.NULL ) :
                new CompleteStatement( subject, predicate, ( Literal ) object,
                    Context.NULL );
        }
    }
    
    // Given a resource as starting point, possibly a predicate and object
    // read from the statement, this will iterate through the statements
    // and return triples.
    private class ResourceToTripleIterator
        extends NestingIterator<Object[], Object[]>
    {
        private Iterator<Iterator<Object[]>> subIterator;
        
        private ResourceToTripleIterator( Node resource, String predicateOrNull,
            Direction direction )
        {
            super( Arrays.asList( new Object[ 0 ],
                new Object[ 0 ] ).iterator() );
            
            Collection<Iterator<Object[]>> iterators =
                new ArrayList<Iterator<Object[]>>();
            iterators.add( new ResourceToObjectTripleIterator( resource, 
                predicateOrNull, direction ) );
            iterators.add( new ResourceToLiteralTripleIterator( resource,
                predicateOrNull ) );
            this.subIterator = iterators.iterator();
        }

        @Override
        protected Iterator<Object[]> createNestedIterator( Object[] item )
        {
            return subIterator.next();
        }
    }
    
    private class ResourceToObjectTripleIterator
        extends PrefetchingIterator<Object[]>
    {
        private Node resource;
        private Iterator<Relationship> predicates;
        private Direction direction;
        
        private ResourceToObjectTripleIterator( Node resource,
            String predicateOrNull, Direction direction )
        {
            this.resource = resource;
            this.direction = direction;
            if ( predicateOrNull == null )
            {
                predicates = resource.getRelationships( direction ).iterator();
            }
            else
            {
                predicates = resource.getRelationships(
                    relType( predicateOrNull ), direction ).iterator();
            }
        }

        @Override
        protected Object[] fetchNextOrNull()
        {
            if ( !predicates.hasNext() )
            {
                return null;
            }
            
            Relationship relationship = predicates.next();
            String predicate = relationship.getType().name();
            Node otherNode = relationship.getOtherNode( resource );
            Node subjectNode = direction == Direction.OUTGOING ?
                resource : otherNode;
            Node objectNode = direction == Direction.OUTGOING ?
                otherNode : resource;
                
            return relationship == null ? null : new Object[] {
                subjectNode, predicate, objectNode,
            };
        }
    }
    
    private static Predicate<String> NOT_URI_PROPERTY = new Predicate<String>()
    {
        public boolean accept( String item )
        {
            return !item.equals( AbstractUriBasedExecutor.URI_PROPERTY_KEY );
        }
    };
    
    private class ResourceToLiteralTripleIterator
        extends PrefetchingIterator<Object[]>
    {
        private Node resource;
        private Iterator<String> predicates;
        private String currentPredicate;
        private Iterator<Object> currentPredicateIterator;
        
        private ResourceToLiteralTripleIterator( Node resource,
            String predicateOrNull )
        {
            this.resource = resource;
            this.predicates = predicateOrNull == null ?
                getResourceLiteralPredicates( resource ) :
                Arrays.asList( predicateOrNull ).iterator();
        }
        
        private Iterator<String> getResourceLiteralPredicates( Node resource )
        {
            return new FilteringIterator<String>(
                resource.getPropertyKeys().iterator(), NOT_URI_PROPERTY );
        }

        @Override
        protected Object[] fetchNextOrNull()
        {
            if ( currentPredicateIterator == null ||
                !currentPredicateIterator.hasNext() )
            {
                if ( predicates.hasNext() )
                {
                    currentPredicate = predicates.next();
                    currentPredicateIterator = new LiteralPredicateIterator(
                        resource, currentPredicate );
                }
            }
            
            Object value = currentPredicateIterator != null &&
                currentPredicateIterator.hasNext() ?
                currentPredicateIterator.next() : null;
            return value == null ? null : newTriple( value );
        }
        
        private Object[] newTriple( Object value )
        {
            return new Object[] { this.resource, this.currentPredicate, value };
        }
    }
    
    private class LiteralPredicateIterator extends PrefetchingIterator<Object>
    {
        private Iterator<Object> values;
        
        private LiteralPredicateIterator( Node resource, String predicate )
        {
            Object rawValue = resource.getProperty( predicate, null );
            this.values = rawValue == null ? null :
                graphDbUtil.propertyValueAsList( rawValue ).iterator();
        }

        @Override
        protected Object fetchNextOrNull()
        {
            return values != null && values.hasNext() ? values.next() : null;
        }
    }
    
    private class ObjectFilteredIterator extends FilteringIterator<Object[]>
    {
        private ObjectFilteredIterator( Iterator<Object[]> source,
            final Value object )
        {
            super( source, new Predicate<Object[]>()
            {
                public boolean accept( Object[] triple )
                {
                    Value tripleObject = getValueForObject( triple[ 2 ] );
                    return object.equals( tripleObject );
                }
            });
        }
    }
    
    private class LiteralsToTriplesIterator
        extends NestingIterator<Object[], Node>
    {
        private String predicateOrNull;
        
        private LiteralsToTriplesIterator( Iterator<Node> source,
            String predicateOrNull )
        {
            super( source );
            this.predicateOrNull = predicateOrNull;
        }

        @Override
        protected Iterator<Object[]> createNestedIterator( Node resource )
        {
            return new ResourceToLiteralTripleIterator( resource,
                predicateOrNull );
        }
    }
}
