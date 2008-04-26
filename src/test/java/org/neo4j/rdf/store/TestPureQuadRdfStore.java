package org.neo4j.rdf.store;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.neometa.structure.DatatypeClassRange;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureClassRange;
import org.neo4j.neometa.structure.MetaStructureImpl;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;

public class TestPureQuadRdfStore extends StoreTestCase
{
    private static final String URI_HUMAN_CLASS = "http://human";
    private static final String URI_NAME_PROPERTY = "http://name";
    private static final String URI_KNOWS_PROPERTY = "http://knows";
    
    public void testStuff() throws Exception
    {
        MetaStructure meta = new MetaStructureImpl( neo() );        
        MetaStructureClass humanClass = meta.getGlobalNamespace().getMetaClass(
            URI_HUMAN_CLASS, true );        
        MetaStructureProperty nameProperty = meta.getGlobalNamespace().
            getMetaProperty( URI_NAME_PROPERTY, true );
        nameProperty.setRange( new DatatypeClassRange( String.class ) );
        humanClass.getDirectProperties().add( nameProperty );
        MetaStructureProperty knowsProperty = meta.getGlobalNamespace().
            getMetaProperty( URI_KNOWS_PROPERTY, true );
        knowsProperty.setRange( new MetaStructureClassRange( humanClass ) );
        humanClass.getDirectProperties().add( knowsProperty );
                       
        PureQuadRdfStore store = new PureQuadRdfStore( neo(), meta,
            new WillBeQuadRepresentationStrategy( neo(), meta ) );
        
        Uri emilsUri = new Uri( "http://emil" );
        Set<String> emilsNames = new HashSet<String>(
            Arrays.asList( "Emil", "Empa" ) );
        Set<String> emilsFriends = new HashSet<String>( Arrays.asList(
            "http://mattias", "http://johan" ) );
        
        for ( String name : emilsNames )
        {
            store.addStatements( new CompleteStatement( emilsUri, new Uri(
                URI_NAME_PROPERTY ), new Literal( name ) ) );            
        }
        for ( String friend : emilsFriends )
        {
            store.addStatements( new CompleteStatement( emilsUri, new Uri(
                URI_KNOWS_PROPERTY ), new Uri( friend ) ) );            
        }
        
        // Make sure that the right names are returned
        for ( Statement statement : store.getStatements(
            new WildcardStatement( emilsUri, new Uri( URI_NAME_PROPERTY ),
                new Wildcard( "name" ) ), true ) )
        {
            assertTrue( emilsNames.remove( ( ( Literal )
                statement.getObject() ).getValue() ) );
        }
        assertTrue( emilsNames.isEmpty() );

        // Make sure the right friends are returned
        for ( Statement statement : store.getStatements(
            new WildcardStatement( emilsUri, new Uri( URI_KNOWS_PROPERTY ),
                new Wildcard( "friend" ) ), true ) )
        {
            assertTrue( emilsFriends.remove( ( ( Uri )
                statement.getObject() ).getUriAsString() ) );
        }
        assertTrue( emilsFriends.isEmpty() );
    }
}
