package org.neo4j.rdf.model;

public class SubjectImpl implements Subject
{
	private String uri;
	
	public SubjectImpl( String uriOrNull )
	{
		this.uri = uriOrNull;
	}

	public String uriAsString()
	{
		return this.uri;
	}
	
	@Override
	public String toString()
	{
		return "Subject[" + uriAsString() + "]";
	}
}
