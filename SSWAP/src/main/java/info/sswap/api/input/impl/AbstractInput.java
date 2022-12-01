/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.Input;
import info.sswap.api.input.InputValue;
import info.sswap.api.input.PropertyInput;

import java.io.Serializable;
import java.net.URI;

import com.google.common.base.Objects;

/**
 * @author Evren Sirin
 */
public abstract class AbstractInput implements Input, Serializable {
	protected final URI type;
	protected InputValue value;
	protected String label;
	protected String description;
	protected PropertyInput propertyInput;

	public AbstractInput(URI type) {
	    this.type = type;
	    this.label = "";
    }
	
	/**
	 * Checks the equality of generic fields value, label, description.
	 */
	protected boolean genericEqualTo(Input that) {
		return Objects.equal(this.value, that.getValue())
		       && Objects.equal(this.label, that.getLabel())
		       && Objects.equal(this.description, that.getDescription());
	}
	
	public PropertyInput getPropertyInput() {
		return propertyInput;
	}
	
	public void setPropertyInput(PropertyInput propertyInput) {
		this.propertyInput = propertyInput;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public URI getType() {
		return type;
	}

	@Override
    public String getDescription() {
	    return description;
    }

	@Override
    public void setDescription(String description) {
	    this.description = description;
    }

	@Override
    public String getLabel() {	    
	    return label;
    }

	@Override
    public void setLabel(String label) {
	    this.label = label;
    }
	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputValue getValue() {
    	return value;
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setValue(InputValue value) {
    	this.value = value;
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isUnrestricted() {
		return false;
	}
	
	protected String toStringLabel() {
		return description == null ? label : label + " '" + description + "'";		
	}
}
