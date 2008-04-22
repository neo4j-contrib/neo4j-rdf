package org.neo4j.rdf.model;

/**
 * Represents a triple statement with subject, predicate and object.
 */
public interface Statement
{
    /**
     * @return the subject ({@link Subject}) of this statement, f.ex: TODO
     */
    Subject getSubject();

    /**
     * @return the predicate ({@link Predicate}) of this statement, f.ex: TODO
     */
    Predicate getPredicate();
    
    /**
     * @return the object ({@link TripleObject}) of this statement, f.ex: TODO
     */
    TripleObject getObject();
}
