package org.neo4j.triplestore;

import java.net.URI;

/**
 * Test of an interface to a triple store.
 */
public interface TripleStore
{
	/**
	 * Writes one statement to the store.
	 * @param subject the subject.
	 * @param predicate the predicate/property.
	 * @param object the object/value.
	 */
	void writeStatement( URI subject, String predicate, String object );
	
	/**
	 * Writes one statement to the store.
	 * @param subject the subject.
	 * @param predicate the predicate/property.
	 * @param objectUri the {@link URI} to the object.
	 */
	void writeStatement( URI subject, String predicate, URI objectUri );
	
	/**
	 * Deletes one statement from the store.
	 * @param subject the subject.
	 * @param predicate the predicate/property.
	 * @param object the object/value.
	 */
	void deleteStatement( URI subject, String predicate, String object );

	/**
	 * Deletes one statement from the store.
	 * @param subject the subject.
	 * @param predicate the predicate/property.
	 * @param objectUri the {@link URI} to the object.
	 */
	void deleteStatement( URI subject, String predicate, URI objectUri );

	// TODO Add more methods, querying
}
