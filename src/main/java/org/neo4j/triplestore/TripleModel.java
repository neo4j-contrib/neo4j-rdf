package org.neo4j.triplestore;

import org.neo4j.api.core.Node;

/**
 * Responsible for connections between subjects, predicates and objects.
 */
public interface TripleModel
{
	/**
	 * Performs the actual connection between a subject and an object via
	 * a predicate.
	 * @param subject the subject in the statement.
	 * @param predicate the predicate in the statement.
	 * @param dataValue the object (data value) in the statement.
	 */
	void connect( Node subject, String predicate, Object dataValue );

	/**
	 * Performs the actual connection between a subject and an object via
	 * a predicate.
	 * @param subject the subject in the statement.
	 * @param predicate the predicate in the statement.
	 * @param object the object in the statement.
	 */
	void connect( Node subject, String predicate, Node object );
	
	/**
	 * Performs the actual disconnection of a subject from an object via
	 * a predicate.
	 * @param subject the subject in the statement.
	 * @param predicate the predicate in the statement.
	 * @param dataValue the object (data value) in the statement.
	 */
	void disconnect( Node subject, String predicate, Object dataValue );
	
	/**
	 * Performs the actual disconnection of a subject from an object via
	 * a predicate.
	 * @param subject the subject in the statement.
	 * @param predicate the predicate in the statement.
	 * @param object the object in the statement.
	 */
	void disconnect( Node subject, String predicate, Node object );

	// TODO Add more methods, querying
}
