package org.neo4j.rdf.newmodel;

public class Statement
{
    private final Resource subject;
    private final Uri predicate;
    private final Value object;

    public Statement( Resource subject, Uri predicate, Value object )
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }
    
    public Resource getSubject()
    {
        return this.subject; 
    }
    
    public Uri getPredicate()
    {
        return this.predicate;
    }
    
    public Value getObject()
    {
        return this.object;
    }
}
