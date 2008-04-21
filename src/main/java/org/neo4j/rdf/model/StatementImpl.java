package org.neo4j.rdf.model;

public class StatementImpl implements Statement
{
	private Subject subject;
	private Predicate predicate;
	private TripleObject object;
	
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
