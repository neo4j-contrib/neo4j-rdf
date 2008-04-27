package org.neo4j.rdf.store;

import java.util.List;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Uri;

public class TestVerboseQuadStore extends StoreTestCase
{
    public void testWeirdCases() throws Exception
    {
        VerboseQuadStore store = new VerboseQuadStore( neo(), indexService(),
            null );
        Uri r1 = new Uri( "http://emil" );
        Uri r2 = new Uri( "http://mattias" );
        Context c1 = new Context( "http://context1" );
        Context c2 = new Context( "http://context2" );

        CompleteStatement sEmilNameC1 =
            new CompleteStatement( r1, NAME, new Literal( "Emil" ), c1 );
        CompleteStatement sEmilKnowsC1 =
            new CompleteStatement( r1, KNOWS, r2, c1 );
        CompleteStatement sEmilNameC2 =
            new CompleteStatement( r1, NAME, new Literal( "Emil" ), c2 );
        CompleteStatement sEmilKnowsC2 =
            new CompleteStatement( r1, KNOWS, r2, c2 );
        CompleteStatement sStupidC1 =
            new CompleteStatement( r1, r1, r1, new Context( r1.getUriAsString() ) );
        List<CompleteStatement> statements = addStatements( store,
                sEmilNameC1
                ,
                sEmilKnowsC1
                ,
                sEmilNameC2
                ,
                sEmilKnowsC2
                ,
                sStupidC1
                );

        removeStatements( store, statements );
        deleteEntireNodeSpace();
    }
}
