/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.SSWAPLiteral;

import java.net.URI;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;

/**
 * Implementation of SSWAPElement that is a literal.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class Literal extends ElementImpl implements SSWAPLiteral {

	/**
	 * The value of the literal.
	 */
	private String value;

	private URI datatypeURI;

	private String language;

	/**
	 * Initializes the literal with the given value
	 * 
	 * @param value
	 *            the value of the literal
	 */
	public Literal(String value) {
		this.value = value;
	}

	/**
	 * Creates a new literal with value, type information (optional) and language information (optional).
	 *  
	 * @param value the value of the literal (its lexical representation)
	 * @param datatypeURI the URI of the datatype (may be null)
	 * @param language the declared language of the literal (may be null)
	 * @throws IllegalArgumentException if the lexical representation of the value is not valid given its declared XSD datatype.
	 */
	public Literal(String value, URI datatypeURI, String language) throws IllegalArgumentException {
		this.value = value;
		this.datatypeURI = datatypeURI;
		this.language = language;
		
		assertValidValue(value, datatypeURI);
	}
	
	/**
	 * A copy constructor for a literal
	 * @param other the literal to be copied
	 */
	Literal(Literal other) {
		this.value = other.value;
		this.datatypeURI = other.datatypeURI;
		this.language = other.language;		
	}
	
	/**
	 * Checks whether the value of the literal conforms to its declared XSD datatype. The check is only performed when
	 * both value and datatypeURI are not null.
	 * 
	 * @param value the value to be checked (the lexical representation of the value)
	 * @param datatypeURI the URI of the XSD datatype
	 * @throws IllegalArgumentException if the value does not conform to its declared XSD datatype.
	 */
	private void assertValidValue(String value, URI datatypeURI) throws IllegalArgumentException {
		if ((datatypeURI != null) && (value != null)) {
			// verify that the value conforms to the datatype
			RDFDatatype rdfDatatype = TypeMapper.getInstance().getTypeByName(datatypeURI.toString());
			
			if ((rdfDatatype != null) && !rdfDatatype.isValid(value)) {
				throw new IllegalArgumentException(String.format("The value \"%s\" is not a valid value for the datatype %s", value, datatypeURI.toString()));
			}
		}
	}

	/**
	 * Gets the RDF identifier of the literal (method required by the interface). Since literals do not have any
	 * identifiers, this method always returns null.
	 * 
	 * @return always null
	 */
	@SuppressWarnings("unchecked")
	public RdfKey getRdfId() {
		return null;
	}

	/**
	 * Sets the RDF identifier (method required by the interface). Since literals do not have any identifiers, this
	 * method always throws UnsupportedOperationException.
	 * 
	 * @throws UnsupportedOperationException
	 *             always
	 * @param rdfId
	 *            the RDF identifier
	 */
	@SuppressWarnings("unchecked")
	public void setRdfId(RdfKey rdfId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Notifies the caller that this SSWAPElement is a literal.
	 * 
	 * @return always true
	 */
	@Override
	public boolean isLiteral() {
		return true;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPLiteral asLiteral() {
		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Boolean asBoolean() {
		if ("true".equalsIgnoreCase(value)) {
			return Boolean.TRUE;
		}
		else if ("false".equalsIgnoreCase(value)) {
			return Boolean.FALSE;
		}

		return null;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Double asDouble() {
		if (value != null) {
			try {
				return Double.parseDouble(value);
			}
			catch (NumberFormatException e) {
				return null;
			}
		}

		return null;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Integer asInteger() {
		if (value != null) {
			try {
				return Integer.parseInt(value);
			}
			catch (NumberFormatException e) {
				return null;
			}
		}

		return null;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String asString() {
		return value;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return the datatypeURI
	 */
	public URI getDatatypeURI() {
		return datatypeURI;
	}


	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}	
	  
    /**
     * @inheritDoc
     */
	@Override
    public void addLabel(String label) {
    	throw new IllegalArgumentException("Unable to add a label to a literal");
    }
    
    /**
     * @inheritDoc
     */
	@Override
    public void addComment(String comment) {
		throw new IllegalArgumentException("Unable to add a comment a literal");
    }
	
	@Override
	public boolean equals(Object o) {
		// the following is for performance reasons
		if (this == o) {
			return true;
		}

		// (NOTE: instanceof returns false for nulls)
		if (o instanceof Literal) {
			Literal other = (Literal) o;

			// the values must match
			if (value != null) {
				if (!value.equals(other.value)) {
					return false;
				}
			}
			else if (other.value != null) {
				return false;
			}
			
			// the datatypeURI must match
			if (datatypeURI != null) {
				if (!datatypeURI.equals(other.datatypeURI)) {					
					return false;
				}				
			}
			else if (other.datatypeURI != null) {
				return false;
			}
			
			// the languages must match
			if (language != null) {
				if (!language.equals(other.language)) {
					return false;
				}				
			}
			else if (other.language != null) {
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
		int result = ((value == null)? 0 : value.hashCode());
		
		result = ODD_PRIME * result + ((datatypeURI == null)? 0 : datatypeURI.hashCode());
		result = ODD_PRIME * result + ((language == null)? 0 : language.hashCode());		 
		
		return result;
	}
}
