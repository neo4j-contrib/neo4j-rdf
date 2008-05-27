package org.neo4j.rdf.model;

/**
 * An RDF quad statement (subject, predicate, object, context). Subject is a
 * {@link Resource} or a {@link Wildcard}. Predicate is a {@link Uri} or a
 * {@link Wildcard}. Object is a {@link Resource} (so-called "object property"),
 * a {@link Literal} (so-called "data property") or a {@link Wildcard}. Context
 * is a {@link Context} (including the special context {@link Context#NULL}
 * for the "default graph") or a {@link Wildcard}.
 * <p>
 * Please note that no method in this interface is allowed to return
 * <code>null</code>. 
 */
public interface Statement
{
    /**
     * The subject of this statement, which is either a {@link Resource} or a
     * {@link Wildcard}.
     * @return the subject of this statement
     */
    Value getSubject();
    /**
     * The predicate of this statement, which is either a {@link Uri} or a
     * {@link Wildcard}.
     * @return the predicate of this statement
     */
    Value getPredicate();
    /**
     * The object of this statement, which is either a {@link Resource},
     * a {@link Literal} or a {@link Wildcard}.
     * @return the object of this statement
     */
    Value getObject();    
    /**
     * The single context (aka named graph) for this quad statement. Valid
     * values are:
     * <ol>
     * <li>a {@link Context} for a specific named graph
     * <li>{@link Context#NULL} for the special "null context" (aka the "default
     * graph")
     * <li>a {@link Wildcard} for any context
     * </ol>
     * Please note that this method must NOT return <code>null</code>.
     * @return the context for this statement
     */    
    Value getContext();
    
    /**
     * Returns this statement as a wildcard statement.
     * @return this statement as a wildcard statement
     */
    WildcardStatement asWildcardStatement();
}