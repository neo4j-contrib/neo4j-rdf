package org.neo4j.rdf.model;

/**
 * Represents the object in a statement (subject, predicate, object).
 */
public interface TripleObject
{
    /**
     * @return whether this object is an object type property or not.
     * If {@code true} the {@link #getResourceOrNull()} should return a
     * valid {@link Uri}, else {@link #getLiteralValueOrNull()} should
     * return a valid literal object.
     */
    boolean isObjectProperty();
    
    /**
     * @return the {@link Uri} representing the resource if this is an
     * object type property.
     */
    Uri getResourceOrNull();
    
    /**
     * @return the literal value if this isn't an object type property.
     */
    Object getLiteralValueOrNull();
}
