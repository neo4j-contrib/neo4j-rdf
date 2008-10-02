package org.neo4j.rdf.model;

/**
 * A blank node (aka bnode, aka anonymous node). A blank node has an identifier
 * to be able to compare it to other blank nodes internally.
 */
public class BlankNode implements Resource
{
    private final String internalId;

    /**
     * Create a blank node with an internal identifier (which may be null).
     * @param internalId the internal identifier, or null
     */
    public BlankNode( String internalId )
    {
        this.internalId = internalId;
    }
    
    /**
     * Returns the internal identifier of this blank node, which can be null.
     * @return the internal identifier of this blank node or null
     */
    public String getInternalIdOrNull()
    {
        return this.internalId;
    }
    
    /**
     * Returns <code>false</code> (a bnode is not a wildcard).
     * @return <code>false</code>
     */
    public boolean isWildcard()
    {
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return getInternalIdOrNull() == null
            ? getInternalIdOrNull().hashCode()
                : super.hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( getInternalIdOrNull() == null )
        {
            return super.equals( o );
        }
        else if ( o instanceof BlankNode)
        {
            return getInternalIdOrNull().equals( ( ( BlankNode ) o ).
                getInternalIdOrNull() );
        }
        return false;
    }


    @Override
    public String toString()
    {
        return "BlankNode[" + internalId + "]";
    }
}
 