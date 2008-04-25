package org.neo4j.rdf.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.neometa.structure.DatatypeClassRange;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureClassRange;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.neometa.structure.PropertyRange;

/**
 * A validator for {@link OwlInstance} objects. It checks so that they conform
 * to the ontologies. Objects are validated right before a transaction commit
 * which makes it possible to have objects in the middle of a transaction which
 * right then and there doesn't conform to the ontologies. The only thing that
 * matter is that they conform before they are committed.
 */
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
    
    private MetaStructure meta;
    
    public Validator( MetaStructure meta )
    {
        this.meta = meta;
    }
	
	/**
	 * Validates an {@link OwlInstance}. It uses {@link OwlModel}
	 * ({@link IdmNeoRepo#owlModel()} model) to get the restrictions. 
	 * 
	 * @param instance the instance to validate.
	 * @throws Exception if <code>instance</code> doesn't validate.
	 */
	public void validate( Validatable instance ) throws Exception
	{
	    ValidationContext context = new ValidationContext( instance );
	    
		// Gather which properties regards to this instance
		Set<MetaStructureProperty> properties =
		    new HashSet<MetaStructureProperty>();
		for ( MetaStructureClass cls : context.getClasses() )
		{
			properties.addAll( cls.getAllProperties() );
		}
		
		// Check cardinality and range on all properties
		for ( MetaStructureProperty property : properties )
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
			MetaStructureProperty property =
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
		MetaStructureProperty metaProperty ) throws Exception
	{
		String key = metaProperty.getName();
		PerPropertyContext pContext = context.property( metaProperty );
		PropertyRange propertyRange = pContext.getRange();
		Validatable instance = context.validatable;
		if ( propertyRange == null )
		{
			return;
		}
		else if ( propertyRange instanceof MetaStructureClassRange )
		{
		    MetaStructureClassRange classRange =
		        ( MetaStructureClassRange ) propertyRange;
		    for ( MetaStructureClass rangeClass : classRange.getRangeClasses() )
		    {
    			for ( Validatable property : instance.complexProperties( key ) )
    			{
    				boolean found = false;
    				for ( MetaStructureClass propertyCls :
    				    property.getClasses() )
    				{
    				    // TODO
//    					if ( rangeClass.isAssignableFrom( propertyCls ) )
//    					{
//    						found = true;
//    						break;
//    					}
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
//		else if ( propertyRange instanceof Collection )
//		{
//			if ( instance.hasProperty( key ) )
//			{
//				// DataRange
//				Collection<?> dataRange =
//					( Collection<?> ) propertyRange;
//				for ( Object property : instance.getProperties( key ) )
//				{
//					if ( !dataRange.contains( property ) )
//					{
//						throw new IdmException( "Property " + key +
//							" has an invalid value (" + property +
//							"), expected (one of) " + dataRange );
//					}
//				}
//			}
//		}
//		else if ( propertyRange instanceof OwlInstance )
//		{
//			OwlInstance expectedInstance = ( OwlInstance ) propertyRange;
//			for ( Validatable value : instance.complexProperties( key ) )
//			{
//				if ( !expectedInstance.equals( value ) )
//				{
//					throw new IdmException( "Property " + key +
//						" should have the value of instance " +
//						expectedInstance + ", but has the value " + value );
//				}
//			}
//		}
		else
		{
			throw new RuntimeException( "Couldn't validate " + instance +
				", unknown property range " + propertyRange + " (" +
				propertyRange.getClass() + ")" );
		}
	}

	private void validatePropertyIsWithinCardinality( ValidationContext context,
		MetaStructureProperty property ) throws Exception
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
	    private MetaStructureClass[] instanceOfClasses;
	    private Map<MetaStructureProperty,
	        PerPropertyContext> propertyContexts =
	            new HashMap<MetaStructureProperty, PerPropertyContext>();
	    
	    ValidationContext( Validatable validatable )
	    {
	        this.validatable = validatable;
	    }
	    
	    MetaStructureClass[] getClasses()
	    {
	        if ( instanceOfClasses == null )
	        {
	            Collection<MetaStructureClass> list =
	                validatable.getClasses();
	            instanceOfClasses = list.toArray(
	                new MetaStructureClass[ list.size() ] );
	        }
	        return instanceOfClasses;
	    }
	    
	    PerPropertyContext property( MetaStructureProperty property )
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
	    private MetaStructureProperty property;
	    private PropertyRange range;
	    private Integer minCardinality;
	    private Integer maxCardinality;
	    
	    PerPropertyContext( ValidationContext context,
	        MetaStructureProperty property )
	    {
	        this.context = context;
	        this.property = property;
	    }
	    
	    PropertyRange getRange()
	    {
	        if ( range == null )
	        {
	            range = meta.lookup( property,
	                MetaStructure.LOOKUP_PROPERTY_RANGE, context.getClasses() );
	        }
	        return range;
	    }
	    
	    Integer getMinCardinality()
	    {
            if ( minCardinality == null )
            {
                minCardinality = meta.lookup( property,
                    MetaStructure.LOOKUP_MIN_CARDINALITY,
                    context.getClasses() );
            }
            return minCardinality;
	    }

        Integer getMaxCardinality()
        {
            if ( maxCardinality == null )
            {
                maxCardinality = meta.lookup( property,
                    MetaStructure.LOOKUP_MAX_CARDINALITY,
                    context.getClasses() );
            }
            return maxCardinality;
        }
	}
}
