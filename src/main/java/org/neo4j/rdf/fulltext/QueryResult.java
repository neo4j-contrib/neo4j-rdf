package org.neo4j.rdf.fulltext;

import org.neo4j.rdf.model.CompleteStatement;

/**
 * Holds a query result from a fulltext search.
 */
public class QueryResult
{
	private CompleteStatement statement;
	private double score;
	private String snippet;
	
	public QueryResult( CompleteStatement statement, double score,
		String snippet )
	{
		this.statement = statement;
		this.score = score;
		this.snippet = snippet;
	}
	
	public CompleteStatement getStatement()
	{
		return this.statement;
	}
	
	public double getScore()
	{
		return this.score;
	}
	
	public String getSnippet()
	{
		return this.snippet;
	}
}
