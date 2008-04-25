package org.neo4j.rdf.validation;

import java.util.Collection;

import org.neo4j.neometa.structure.MetaStructureClass;

/**
 * All items which can be validated have to implement this interface.
 */
public interface Validatable
{
	/**
	 * @return the node types for this entity.
	 */
	Collection<MetaStructureClass> getClasses();
	
	/**
	 * @return all the property keys.
	 */
	String[] getAllPropertyKeys();
	
	/**
	 * @param key the property key.
	 * @return wether or not this entity has the property {@code key}.
	 */
	boolean hasProperty( String key );
	
	/**
	 * @param key the property key.
	 * @return the values for the property key.
	 */
	Object[] getProperties( String key );
	
	/**
	 * @param key the property key.
	 * @param direction the direction.
	 * @return the complex entities for key.
	 */
	Collection<? extends Validatable> complexProperties( String key );
}
