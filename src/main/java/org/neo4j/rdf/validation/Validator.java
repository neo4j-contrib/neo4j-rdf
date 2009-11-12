package org.neo4j.rdf.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.meta.model.ClassRange;
import org.neo4j.meta.model.DatatypeClassRange;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelProperty;
import org.neo4j.meta.model.PropertyRange;

public class Validator
{
    //	private void debug( Validatable instance, String string )
    //	{
    //		debug( instance, string, true );
    //	}
    //	
    //	private void debug( Validatable instance, String string, boolean doit )
    //	{
    //		if ( instance instanceof MessageOperation && doit )
    //		{
    //			System.out.println( string );
    //		}
    //	}
    
    private MetaModel meta;
    
    public Validator( MetaModel meta )
    {
        this.meta = meta;
    }
    
    public void validate( Validatable instance ) throws Exception
    {
        ValidationContext context = new ValidationContext( instance );
        
        // Gather which properties regards to this instance
        Set<MetaModelProperty> properties =
            new HashSet<MetaModelProperty>();
        for ( MetaModelClass cls : context.getClasses() )
        {
            properties.addAll( cls.getAllProperties() );
        }
        
        // Check cardinality and range on all properties
        for ( MetaModelProperty property : properties )
        {
            validatePropertyIsWithinCardinality( context, property );
            validatePropertyRange( context, property );
        }
        
        // Check so that the properties on the instance are valid properties
        // from the ontology
        Set<String> existingPropertyKeys = new HashSet<String>(
            Arrays.asList( instance.getAllPropertyKeys() ) );
        for ( String key : existingPropertyKeys )
        {
            validatePropertyExists( instance, key );
            MetaModelProperty property =
                meta.getGlobalNamespace().getMetaProperty( key, false );
            if ( !properties.contains( property ) )
            {
                throw new Exception( "Property '" + key +
                    "' isn't allowed on " + instance );
            }
        }
    }
    
    private void validatePropertyExists( Validatable instance, String key )
        throws Exception
    {
        if ( meta.getGlobalNamespace().getMetaProperty( key, false ) == null )
        {
            throw new Exception( "Invalid property " + key + " on " +
                instance );
        }
    }
    
    private void validatePropertyRange( ValidationContext context,
        MetaModelProperty metaProperty ) throws Exception
    {
        String key = metaProperty.getName();
        PerPropertyContext pContext = context.property( metaProperty );
        PropertyRange propertyRange = pContext.getRange();
        Validatable instance = context.validatable;
        if ( propertyRange == null )
        {
            return;
        }
        else if ( propertyRange instanceof ClassRange )
        {
            ClassRange classRange = ( ClassRange ) propertyRange;
            for ( MetaModelClass rangeClass : classRange.getRangeClasses() )
            {
                for ( Validatable property : instance.complexProperties( key ) )
                {
                    boolean found = false;
                    for ( MetaModelClass propertyCls :
                        property.getClasses() )
                    {
//                        TODO
//                        if ( rangeClass.isAssignableFrom( propertyCls ) )
//                        {
//                            found = true;
//                            break;
//                        }
                    }
                    if ( !found )
                    {
                        throw new Exception( "Property " + key +
                            " has an invalid value type for " + instance +
                            ", expected " + rangeClass );
                    }
                }
            }
        }
        else if ( propertyRange instanceof DatatypeClassRange )
        {
            if ( instance.hasProperty( key ) )
            {
                Class<?> rangeClass = ( ( DatatypeClassRange )
                    propertyRange ).getRangeClass();
                for ( Object property : instance.getProperties( key ) )
                {
                    if ( !rangeClass.equals( property.getClass() ) )
                    {
                        throw new Exception( "Property " + key +
                            " has an invalid type, expected " + rangeClass +
                            ", but was " + property.getClass() );
                    }
                }
            }
        }
//        else if ( propertyRange instanceof Collection )
//        {
//            if ( instance.hasProperty( key ) )
//            {
//                // DataRange
//                Collection<?> dataRange =
//                    ( Collection<?> ) propertyRange;
//                for ( Object property : instance.getProperties( key ) )
//                {
//                    if ( !dataRange.contains( property ) )
//                    {
//                        throw new IdmException( "Property " + key +
//                            " has an invalid value (" + property +
//                            "), expected (one of) " + dataRange );
//                    }
//                }
//            }
//        }
//        else if ( propertyRange instanceof OwlInstance )
//        {
//            OwlInstance expectedInstance = ( OwlInstance ) propertyRange;
//            for ( Validatable value : instance.complexProperties( key ) )
//            {
//                if ( !expectedInstance.equals( value ) )
//                {
//                    throw new IdmException( "Property " + key +
//                        " should have the value of instance " +
//                        expectedInstance + ", but has the value " + value );
//                }
//            }
//        }
        else
        {
            throw new RuntimeException( "Couldn't validate " + instance +
                ", unknown property range " + propertyRange + " (" +
                propertyRange.getClass() + ")" );
        }
    }
    
    private void validatePropertyIsWithinCardinality( ValidationContext context,
        MetaModelProperty property ) throws Exception
    {
        String key = property.getName();
        PerPropertyContext pContext = context.property( property );
        Integer minCardinality = pContext.getMinCardinality();
        Integer maxCardinality = pContext.getMaxCardinality();
        if ( maxCardinality == null && minCardinality == null )
        {
            String functionality =
                property.getAdditionalProperty( "functionality" );
            if ( functionality != null )
            {
                maxCardinality = 1;
                minCardinality = 0;
            }
            else
            {
                return;
            }
        }
        
        int valueCardinality = 0;
        PropertyRange range = pContext.getRange();
        if ( range == null )
        {
            throw new RuntimeException( "Property " + property +
            " is neither DatatypeProperty nor ObjectProperty" );
        }
        else if ( range.isDatatype() )
        {
            // TODO
            if ( context.validatable.hasProperty( key ) )
            {
                valueCardinality =
                    context.validatable.getProperties( key ).length;
            }
        }
        else
        {
            // TODO
            valueCardinality =
                context.validatable.complexProperties( key ).size();
        }
        
        if ( minCardinality != null && valueCardinality < minCardinality )
        {
            throw new Exception( "Cardinality for property " + key + " is " +
                valueCardinality + ", minimum required is " + minCardinality );
        }
        if ( maxCardinality != null && valueCardinality > maxCardinality )
        {
            throw new Exception( "Cardinality for property " + key + " is " +
                valueCardinality + ", maximum allowed is " + maxCardinality );
        }
    }
    
    private class ValidationContext
    {
        private Validatable validatable;
        private MetaModelClass[] instanceOfClasses;
        private Map<MetaModelProperty,
        PerPropertyContext> propertyContexts =
            new HashMap<MetaModelProperty, PerPropertyContext>();
        
        ValidationContext( Validatable validatable )
        {
            this.validatable = validatable;
        }
        
        MetaModelClass[] getClasses()
        {
            if ( instanceOfClasses == null )
            {
                Collection<MetaModelClass> list =
                    validatable.getClasses();
                instanceOfClasses = list.toArray(
                    new MetaModelClass[ list.size() ] );
            }
            return instanceOfClasses;
        }
        
        PerPropertyContext property( MetaModelProperty property )
        {
            PerPropertyContext result = propertyContexts.get( property );
            if ( result == null )
            {
                result = new PerPropertyContext( this, property );
                propertyContexts.put( property, result );
            }
            return result;
        }
    }
    
    private class PerPropertyContext
    {
        private ValidationContext context;
        private MetaModelProperty property;
        private PropertyRange range;
        private Integer minCardinality;
        private Integer maxCardinality;
        
        PerPropertyContext( ValidationContext context,
            MetaModelProperty property )
        {
            this.context = context;
            this.property = property;
        }
        
        PropertyRange getRange()
        {
            if ( range == null )
            {
                range = meta.lookup( property,
                    MetaModel.LOOKUP_PROPERTY_RANGE, context.getClasses() );
            }
            return range;
        }
        
        Integer getMinCardinality()
        {
            if ( minCardinality == null )
            {
                minCardinality = meta.lookup( property,
                    MetaModel.LOOKUP_MIN_CARDINALITY,
                    context.getClasses() );
            }
            return minCardinality;
        }
        
        Integer getMaxCardinality()
        {
            if ( maxCardinality == null )
            {
                maxCardinality = meta.lookup( property,
                    MetaModel.LOOKUP_MAX_CARDINALITY,
                    context.getClasses() );
            }
            return maxCardinality;
        }
    }
}
