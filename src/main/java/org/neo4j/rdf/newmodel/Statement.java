package org.neo4j.rdf.newmodel;

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
     * All the contexts for this statement, or an empty list for none.
     * @return the contexts for this statement
     */
    Iterable<Context> getContexts();
}