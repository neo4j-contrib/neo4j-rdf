package org.neo4j.rdf.store.representation;

import java.util.Iterator;

import junit.framework.TestCase;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.representation.standard.VerboseQuadStrategy;

public class TestRepresentations extends TestCase
{
    public void testVerboseQuadRepresentation() throws Exception
    {
        RepresentationStrategy strategy = new VerboseQuadStrategy( null, null );
        Uri uriA = new Uri( "http://test.com/uriA" );
        Uri uriB = new Uri( "http://test.com/uriB" );
        Uri uriC = new Uri( "http://test.com/uriC" );
        Uri uriD = new Uri( "http://test.com/uriD" );
        Uri uriE = new Uri( "http://test.com/uriE" );
        Statement statement = new CompleteStatement( uriA, uriB, uriC,
            new Context( uriD.getUriAsString() ) );
        AbstractRepresentation representation =
            strategy.getAbstractRepresentation( statement,
                new AbstractRepresentation() );
        assertEquals( 4, countIterable( representation.nodes() ) );
        
        Statement secondStatement = new CompleteStatement( uriA, uriB, uriC,
            new Context( uriE.getUriAsString() ) );
        representation = strategy.getAbstractRepresentation( secondStatement,
            representation );
        assertEquals( 5, countIterable( representation.nodes() ) );
        
        int middleNodeCount = 0;
        int contextNodeCount = 0;
        for ( AbstractNode node : representation.nodes() )
        {
            if ( node.getSingleExecutorInfo(
                VerboseQuadStrategy.EXECUTOR_INFO_NODE_TYPE ).equals(
                    VerboseQuadStrategy.TYPE_MIDDLE ) )
            {
                middleNodeCount++;
            }
            if ( node.getExecutorInfo(
                VerboseQuadStrategy.EXECUTOR_INFO_NODE_TYPE ).contains(
                    VerboseQuadStrategy.TYPE_CONTEXT ) )
            {
                contextNodeCount++;
            }
        }
        assertEquals( 1, middleNodeCount );
        assertEquals( 2, contextNodeCount );
    }
    
    private int countIterable( Iterable<?> iterable )
    {
        int counter = 0;
        Iterator<?> itr = iterable.iterator();
        while ( itr.hasNext() )
        {
            counter++;
            itr.next();
        }
        return counter;
    }
}
