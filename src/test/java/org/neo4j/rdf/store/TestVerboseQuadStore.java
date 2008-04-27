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

        CompleteStatement sEmilName = new CompleteStatement( r1, NAME,
            new Literal( "Emil" ), Context.NULL );
        CompleteStatement sEmilKnows = new CompleteStatement( r1, KNOWS, r2,
            Context.NULL );
        List<CompleteStatement> statements = addStatements( store,
                sEmilName
//                ,
//                sEmilKnows
                );
        removeStatements( store, statements );
        deleteEntireNodeSpace();
    }
}
