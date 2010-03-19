package org.neo4j.rdf.store.representation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Value;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.Neo4jTestCase;
import org.neo4j.rdf.store.representation.standard.VerboseQuadExecutor;
import org.neo4j.rdf.store.representation.standard.VerboseQuadStrategy;

public class TestRepresentations extends Neo4jTestCase
{
    @Test
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
    
    @Test
    public void testVerboseQuadWildcards() throws Exception
    {
        RepresentationExecutor executor = new VerboseQuadExecutor( graphDb(),
            indexService(), null, null );
        RepresentationStrategy strategy =
            new VerboseQuadStrategy( executor, null );
        Value[] s = { new Uri( "http://test.com/uriA" ), new Wildcard( "s" ) };
        Value[] p = { new Uri( "http://test.com/uriB" ) };
        Value[] o = { new Uri( "http://test.com/uriC" ), new Wildcard( "o" ) };
        Value[] g = { new Uri( "http://test.com/uriD" ), new Wildcard( "g" ) };
        for ( Value vs : s )
        {
            for ( Value vp : p )
            {
                for ( Value vo : o )
                {
                    for ( Value vg : g )
                    {
                        Statement statement = null;
                        if ( vs.isWildcard() || vp.isWildcard() ||
                            vo.isWildcard() || vg.isWildcard() )
                        {
                            statement = new WildcardStatement( vs, vp, vo, vg );
                        }
                        else
                        {
                            statement = new CompleteStatement( ( Resource ) vs,
                                ( Uri ) vp, ( Resource ) vo, new Context(
                                    ( ( Uri ) vg ).getUriAsString() ) );
                        }
                        strategy.getAbstractRepresentation( statement,
                            new AbstractRepresentation() );
                    }
                }
            }
        }
    }
}
