/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.net.URI;
import java.net.URISyntaxException;

import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;

import com.clarkparsia.empire.SupportsRdfId;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * Implementation of SSWAPProperty
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class PropertyImpl extends ElementImpl implements SSWAPProperty {

	private URI uri;

	private SSWAPIndividual individual;
	
	/**
	 * The value of the property.
	 */
	private SSWAPElement value;

	/**
	 * Initializes the SSWAPProperty based on the information from a Jena Property (the predicate between SSWAPIndividual
	 * and the value).
	 * 
	 * @param property
	 *            the Jena Property
	 */
	PropertyImpl(SSWAPIndividual individual, Property property) {
		this.individual = individual;
		try {
			this.uri = new URI(property.getURI());
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("The property does not have a valid URI: " + e);
		}
	}

	
	@SuppressWarnings("unchecked")
	public RdfKey getRdfId() {
		if (getURI() != null) {
			return new SupportsRdfId.URIKey(getURI());
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public void setRdfId(RdfKey rdfIdentifier) {
		// nothing
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void setURI(URI uri) {
		this.uri = uri;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public URI getURI() {
		return uri;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPElement getValue() {
		return value;
	}

	void setValue(SSWAPElement value) {
		if (value == null) {
			throw new IllegalArgumentException("An attempt to set a null value in PropertyImpl.setValue(SSWAPElement)");
		}
		
		SourceModel sourceModel = assertSourceModel();
		
		if (sourceModel.isValueValidationEnabled()) {
			if (!getPredicate().isAnnotationPredicate()) {
				if (value.isIndividual() && getPredicate().isDatatypePredicate()) {
					throw new IllegalArgumentException("Attempt to set an individual as a value for a datatype property: " + uri.toString());
				}

				if (value.isLiteral() && getPredicate().isObjectPredicate()) {
					throw new IllegalArgumentException("Attempt to set a literal as a value for an object property: " + uri.toString());
				}
			}		
		}
		
		this.value = value;
	}
    
    /**
     * @inheritDoc
     */
    public SSWAPPredicate getPredicate() {
    	SourceModel sourceModel = assertSourceModel();
    	
    	return sourceModel.getPredicate(getURI());
    }
    
    /**
     * @inheritDoc
     */
    public SSWAPIndividual getIndividual() {
    	return individual;
    }
    
    void setIndividual(SSWAPIndividual individual) {
    	this.individual = individual;
    }
    
    public void removeProperty() {
    	if (individual != null) {
    		individual.removeProperty(this);
    	}
    }
    
    @Override
	public boolean equals(Object o) {
		// the following is for performance reasons
		if (this == o) {
			return true;
		}

		// the only equivalent objects are ModelImpls or their subclasses
		// (NOTE: instanceof returns false for nulls)
		if (o instanceof PropertyImpl) {
			PropertyImpl other = (PropertyImpl) o;
			
			// the URIs of the properties must match
			if (!rdfIdEquals(other)) {
				return false;
			}
			
			// the individuals (or lack of there of) must match
			if (individual != null) {
				if (!individual.equals(other.individual)) {
					return false;
				}				
			}
			else if (other.individual != null) {
				return false;
			}
			
			// the values must match
			if (value != null) {
				if (!value.equals(other.value)) {
					return false;
				}
			}
			else if (other.value != null) {
				return false;
			}
			
			return true;
		}

		return false;
	}

    /**
     * An arbitrary odd prime used in the hash function.
     */
    private static final int ODD_PRIME = 31;
    
	/**
	 * Overridden hash code method to make sure that the generated hashcodes are consistent with the overriden equals()
	 * method.
	 * 
	 * @return the hashcode.
	 */
	@Override
	public int hashCode() {
		int result = rdfIdHashCode();
		
		result = ODD_PRIME * result + ((individual == null)? 0 : individual.hashCode());
		result = ODD_PRIME * result + ((value == null)? 0 : value.hashCode());
		
		return result;
	}
}
