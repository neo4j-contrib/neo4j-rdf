package org.neo4j.rdf.fulltext;

import org.neo4j.graphdb.Node;

/**
 * Just a raw query result returned from lucene from a search query.
 * A sort of intermediate format.
 */
public class RawQueryResult implements Comparable<RawQueryResult>
{
    private Node node;
    private double score;
    private String snippet;
    
    public RawQueryResult( Node node, double score, String snippet )
    {
        this.node = node;
        this.score = score;
        this.snippet = snippet;
    }
    
    public Node getNode()
    {
        return this.node;
    }
    
    public double getScore()
    {
        return this.score;
    }
    
    public String getSnippet()
    {
        return this.snippet;
    }
    
    public int compareTo( RawQueryResult other )
    {
        Double scoreDouble = score;
        Double otherScoreDouble = other.score;
        return scoreDouble.compareTo( otherScoreDouble );
    }
}
