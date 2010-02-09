package org.neo4j.rdf.store.representation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.rdf.model.Value;

/**
 * An abstract representation of an arbitrary number of RDF statements in the
 * node space. It is created by a {@link RepresentationStrategy}. An instance
 * of this type is abstract in the sense that it doesn't encapsulate "real"
 * Neo4j nodes but instead {@link AbstractNode abstract nodes} which are purely
 * in-memory. It's important to remember that it's not just a description of the 
 * structure of some statements in general, but rather it represents the
 * structure and properties of some <i>specific</i> statements, i.e. the nodes
 * and relationships are actually populated with real values.
 */
public class AbstractRepresentation
{
    private final Map<Object, AbstractNode> nodeKeys =
        new HashMap<Object, AbstractNode>();
    private final List<AbstractNode> nodes = new ArrayList<AbstractNode>();
    private final List<AbstractRelationship> relationships =
    	new ArrayList<AbstractRelationship>();
    
    /**
     * Adds an {@link AbstractNode} to the representation.
     * @param node the node to add to the representation.
     */
    public void addNode( AbstractNode node )
    {
        if ( !this.nodes.contains( node ) )
        {
            this.nodes.add( node );
            if ( node.getKey() != null )
            {
                this.nodeKeys.put( node.getKey(), node );
            }
        }
    }
    
    public AbstractNode node( Object key )
    {
        return nodeKeys.get( key );
    }
    
    /**
     * Adds an {@link AbstractRelationship} to the representation.
     * @param relationship the relationship to add to the representation.
     */
    public void addRelationship( AbstractRelationship relationship )
    {
        if ( !this.relationships.contains( relationship ) )
        {
            this.relationships.add( relationship );
        }
    }
    
    /**
     * Returns the nodes that build up this statement in the node space 
     * @return the nodes that build up this statement in the node space
     */
    public Iterable<AbstractNode> nodes()
    {
        return Collections.unmodifiableList( nodes );
    }
    
    /**
     * Returns the relationships that connect the nodes of this statement in
     * the node space.<br/>
     * Invariant: the nodes at both ends of each relationship in this set
     * are guaranteed to be part of the set of nodes returned by
     * {@link #nodes()}.
     * @return the relationships that connect this representation of a statement
     */
    public Iterable<AbstractRelationship> relationships()
    {
        return Collections.unmodifiableList( relationships );
    }

    // Maybe add this?
    public Map<AbstractElement, Value> getMappingToRdfValues()
    {
        return Collections.emptyMap();
    }
    
    /**
     * http://en.wikipedia.org/wiki/DOT_language
     * @return this representation as a DOT graph, good for debugging.
     */
    public String toDotFormat()
    {
        CharRef counter = new CharRef();
        counter.ch = 'a';
        Map<AbstractNode, String> nameMap = new HashMap<AbstractNode, String>();
        StringBuffer result = new StringBuffer( "digraph representation\n" );
        result.append( "{\n" );
        for ( AbstractRelationship relationship : relationships() )
        {
            String[] start = getNodeNameAndLabel( nameMap,
                relationship.getStartNode(), counter );
            if ( start[ 1 ] != null )
            {
                result.append( "\t" + start[ 0 ] + " [label=\"" +
                    start[ 1 ] + "\"];\n" );
            }
            
            String[] end = getNodeNameAndLabel( nameMap,
                relationship.getEndNode(), counter );
            if ( end[ 1 ] != null )
            {
                result.append( "\t" + end[ 0 ] + " [label=\"" +
                    end[ 1 ] + "\"];\n" );
            }
            
            result.append( "\t" + start[ 0 ] + " -> " + end[ 0 ] +
                " [label=\"" + relationship.getRelationshipTypeName().substring(
                    relationship.getRelationshipTypeName().length() - 15 ) +
                "\"];\n" );
        }
        result.append( "}" );
        return result.toString();
    }
    
    private String[] getNodeNameAndLabel( Map<AbstractNode, String> nameMap,
        AbstractNode node, CharRef counter )
    {
        String name;
        String label = null;
        if ( !nameMap.containsKey( node ) )
        {
            name = String.valueOf( counter.ch );
            counter.ch++;
            nameMap.put( node, name );
            if ( node.getUriOrNull() != null )
            {
                label = node.getUriOrNull().getUriAsString().substring(
                    node.getUriOrNull().getUriAsString().length() - 15 );
            }
            else if ( node.getWildcardOrNull() != null )
            {
                label = "?" + node.getWildcardOrNull().getVariableName();
            }
            else if ( !node.properties().isEmpty() )
            {
                for ( String key : node.properties().keySet() )
                {
                    for ( Object value : node.properties().get( key ) )
                    {
                        label = label == null ? "'" + value + "'" :
                            ", '" + value + "'";
                    }
                }
            }
        }
        else
        {
            name = nameMap.get( node );
        }
        return new String[] { name, label };
    }
    
    private static class CharRef
    {
        private char ch;
    }
}
