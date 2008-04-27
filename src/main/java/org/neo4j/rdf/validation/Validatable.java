package org.neo4j.rdf.validation;

import java.util.Collection;

import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.rdf.model.Uri;

/**
 * All items which can be validated have to implement this interface.
 */
public interface Validatable
{
    Node getUnderlyingNode();

    Uri getUri();

	/**
	 * @return the node types for this entity.
	 */
	Collection<MetaStructureClass> getClasses();

	/**
	 * @return all the property keys.
	 */
	String[] getAllPropertyKeys();

	String[] getSimplePropertyKeys();

	String[] getComplexPropertyKeys();

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
