package org.neo4j.rdf.store;

import org.junit.Test;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;

public class TestDenseTripleStore extends TripleStoreAbstractTestCase
{
    // TODO Handle the case where S and O are the same, the current
    // representation doesn't handle that.
//    public void testSameSameStatements() throws Exception
//    {
//        Uri uriA = new Uri( BASE_URI + "uriA" );
//        Uri uriB = new Uri( BASE_URI + "uriB" );
//        Uri uriC = new Uri( BASE_URI + "uriC" );
//        Uri[] uris = { uriA, uriB, uriC };
//        
//        Collection<CompleteStatement> statements =
//            new ArrayList<CompleteStatement>();
//        int counter = 0;
//        for ( int a = 0; a < uris.length; a++ )
//        {
//            for ( int b = 0; b < uris.length; b++ )
//            {
//                for ( int c = 0; c < uris.length; c++ )
//                {
//                    CompleteStatement statement = new CompleteStatement(
//                        uris[ a ], uris[ b ], uris[ c ], Context.NULL );
//                    statements.add( statement );
//                    addStatements( statement );
//                    counter++;
//                }
//            }
//        }
//        
//        WildcardStatement wildcardStatement = wildcardStatement(
//            new Wildcard( "s" ), new Wildcard( "p" ), new Wildcard( "o" ),
//            Context.NULL );
//        assertResult( wildcardStatement, statements.toArray(
//            new CompleteStatement[ 0 ] ) );
//        removeStatements( wildcardStatement );
//    }
    
    @Test
    public void testSome() throws Exception
    {
        CompleteStatement mattiasKnowsEmil = completeStatement(
            TestUri.MATTIAS,
            TestUri.FOAF_KNOWS,
            TestUri.EMIL,
            Context.NULL );
        CompleteStatement mattiasName = completeStatement(
            TestUri.MATTIAS,
            TestUri.FOAF_NAME,
            new Literal( "Mattias Persson" ),
            Context.NULL );
        
        addStatements( mattiasKnowsEmil, mattiasName );
        
        assertResult( new WildcardStatement( mattiasKnowsEmil ),
            mattiasKnowsEmil );
        assertResult( new WildcardStatement( mattiasName ), mattiasName );
        assertResult( new WildcardStatement( TestUri.MATTIAS.toUri(),
            new Wildcard( "p" ), new Wildcard( "o" ), new Wildcard( "g" ) ),
            mattiasKnowsEmil, mattiasName );
        
        removeStatements( new WildcardStatement( TestUri.MATTIAS.toUri(),
            new Wildcard( "p" ), new Wildcard( "o" ), new Wildcard( "c" ) ) );
    }
}
