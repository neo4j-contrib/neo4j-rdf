package org.neo4j.rdf.model;

/**
 * Default implementation of {@link Statement}.
 */
public class StatementImpl implements Statement
{
    private Subject subject;
    private Predicate predicate;
    private TripleObject object;

    /**
     * @param subject the subject in this statement.
     * @param predicate the predicate in this statement.
     * @param object the object in this statement.
     */
    public StatementImpl( Subject subject, Predicate predicate,
        TripleObject object )
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public Subject getSubject()
    {
        return this.subject;
    }

    public Predicate getPredicate()
    {
        return this.predicate;
    }

    public TripleObject getObject()
    {
        return this.object;
    }
}
