package org.neo4j.rdf.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.api.core.RelationshipType;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.rdf.store.representation.AbstractRelationship;
import org.neo4j.rdf.store.representation.AbstractRepresentation;
import org.neo4j.rdf.store.representation.RepresentationStrategy;
import org.neo4j.util.matching.PatternNode;

public class PatternGraphBuilder
{
    private final RepresentationStrategy representationStrategy;
    
    public PatternGraphBuilder( RepresentationStrategy representationStrategy )
    {
        this.representationStrategy = representationStrategy;
    }
    
    public Map<AbstractNode, PatternNode> buildPatternGraph(
        AbstractRepresentation representation,
        Map<String, VariableHolder> variableMapping )
    {
        return buildPatternGraph( representation, variableMapping, false );
    }
    
    public Map<AbstractNode, PatternNode> buildPatternGraph(
        AbstractRepresentation representation,
        Map<String, VariableHolder> variableMapping, boolean optional )
    {
        Map<AbstractNode, PatternNode> graph =
            new HashMap<AbstractNode, PatternNode>();
        for ( AbstractNode node : representation.nodes() )
        {
            graph.put( node, this.createPatternNode( node, variableMapping ) );
        }
        
        for ( AbstractRelationship relationship :
            representation.relationships() )
        {
            AbstractNode startNode = relationship.getStartNode();
            AbstractNode endNode = relationship.getEndNode();
            graph.get( startNode ).createRelationshipTo(
                graph.get( endNode ), new DynamicRelationshipType(
                    relationship.getRelationshipTypeName() ), optional );
        }
        
        return graph;
    }
    
    private PatternNode createPatternNode( AbstractNode node,
        Map<String, VariableHolder> variableMapping )
    {
        PatternNode patternNode = null;
        if ( node.isWildcard() )
        {
            Wildcard wildcard = node.getWildcardOrNull();
            patternNode = new PatternNode( wildcard.getVariableName() );
            addVariable( variableMapping, wildcard.getVariableName(),
                VariableHolder.VariableType.URI, patternNode,
                this.representationStrategy.getExecutor().
                    getNodeUriPropertyKey( node ) );
        }
        else
        {
            Uri uri = node.getUriOrNull();
            patternNode = new PatternNode(
                uri == null ? "" : uri.getUriAsString() );
            if ( uri != null )
            {
                patternNode.addPropertyEqualConstraint(
                    this.representationStrategy.getExecutor().
                    getNodeUriPropertyKey( node ), uri.getUriAsString() );
            }
        }
        
        for ( Entry<String, Collection<Object>> entry :
            node.properties().entrySet() )
        {
            for ( Object value : entry.getValue() )
            {
                if ( value instanceof Wildcard )
                {
                    addVariable( variableMapping,
                        ( ( Wildcard ) value ).getVariableName(),
                        VariableHolder.VariableType.LITERAL, patternNode,
                        entry.getKey() );
                }
                else
                {
                    patternNode.addPropertyEqualConstraint(
                        entry.getKey(), value );
                }
            }
        }
        return patternNode;
    }
    
    private void addVariable( Map<String, VariableHolder> variableMapping,
        String variableName, VariableHolder.VariableType type,
        PatternNode patternNode, String propertyKey )
    {
        if ( !variableMapping.containsKey( variableName ) )
        {
            variableMapping.put( variableName,
                new VariableHolder( variableName, type, patternNode,
                    propertyKey ) );
        }
    }
    
    private static class DynamicRelationshipType implements RelationshipType
    {
        private final String name;
        private DynamicRelationshipType( String name )
        {
            this.name = name;
        }
        
        public String name()
        {
            return this.name;
        }
        
    }   
    
    public static class VariableHolder
    {
        public static enum VariableType { LITERAL, URI };
        
        private final VariableType variableType;
        private final String variableName;
        private final PatternNode node;
        private final String propertyKey;
        
        VariableHolder( String variableName, VariableType variableType,
            PatternNode node, String propertyKey )
        {
            this.variableType = variableType;
            this.variableName = variableName;
            this.node = node;
            this.propertyKey = propertyKey;
        }
        
        public String getName()
        {
            return this.variableName;
        }

        public VariableType getVariableType()
        {
            return this.variableType;
        }
        
        String getPropertyKey()
        {
            return this.propertyKey;
        }
        
        PatternNode getNode()
        {
            return this.node;
        }
    }
}
