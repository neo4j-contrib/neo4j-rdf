package org.neo4j.rdf.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.swami.om2.neorepo.sparql.MetaModelProxy;
import org.swami.om2.neorepo.sparql.OwlProperty;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType;

/**
 * An implementation of the {@link MetaModelProxy} interface which is used in
 * the SPARQL engine for neo.
 */
public class MetaModelProxyImpl implements MetaModelProxy
{
    private MetaStructure meta;
    
    public MetaModelProxyImpl( MetaStructure meta )
    {
        this.meta = meta;
    }
    
	/**
	 * @return the property key representing the rdf:about property.
	 */
	public String getAboutKey()
	{
        throw new UnsupportedOperationException();
	}
	
	public String getNodeTypeNameKey()
	{
        throw new UnsupportedOperationException();
	}

	/**
	 * @return the underlying {@link Node} for the {@link NodeType} with the URI
	 * <code>uri</code>. 
	 */
	public Node getClassNode( String uri )
	{
        throw new UnsupportedOperationException();
	}

	/**
	 * @return the number of instances of the {@link NodeType} with URI
	 * <code>uri</code>.
	 */
	public int getCount( String uri )
	{
	    MetaStructureClass cls =
	        meta.getGlobalNamespace().getMetaClass( uri, false );
//	    if ( cls == null )
//	    {
//	        cls = meta.getGlobalNamespace().getMetaProperty( uri, false );
//	    }
	    if ( cls == null )
	    {
	        throw new RuntimeException( "Not found ' " + uri + " '" );
	    }
	    
	    return cls.getInstances().size();
	}
	
	/**
	 * @param subjectUri not used yet
	 * @param predicateUri the name of the property.
	 * @param objectUri not used yet
	 * @return a property definition in the neo meta model.
	 */
	public OwlProperty getOwlProperty( String subjectUri, String predicateUri,
		String objectUri )
	{
	    throw new UnsupportedOperationException();
	}

	/**
	 * @return the {@link RelationshipType} used for connecting
	 * {@link OwlInstance} object to {@link NodeType} objects.
	 */
	public RelationshipType getTypeRelationship()
	{
        throw new UnsupportedOperationException();
	}

	/**
	 * @return <code>true</code> if <code>uri</code> is the URI representing
	 * an RDF type.
	 */
	public boolean isTypeProperty( String uri )
	{
        throw new UnsupportedOperationException();
	}
	
	public Object convertCriteriaStringValueToRealValue( String propertyKey,
		String value )
	{
        throw new UnsupportedOperationException();
	}

	public String getObjectType( String subjectUri,
		String predicateUri )
	{
        throw new UnsupportedOperationException();
	}
	
	public String[] getSubTypes( String type, boolean includeMyself )
	{
	    MetaStructureClass cls =
	        meta.getGlobalNamespace().getMetaClass( type, false );
	    if ( cls == null )
	    {
	        throw new RuntimeException();
	    }
	    Set<String> classes = new HashSet<String>();
	    addType( classes, cls );
	    return classes.toArray( new String[ classes.size() ] );
	}
	
	private void addType( Collection<String> result, MetaStructureClass cls )
	{
		result.add( cls.getName() );
		for ( MetaStructureClass sub : cls.getDirectSubs() )
		{
			addType( result, sub );
		}
	}
}
