// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package info.sswap.api.input;

import java.net.URI;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * @author Evren Sirin
 */
public class InputValidator implements InputVisitor {
	private Set<URI> properties;
	private boolean hasValue;
	
	public InputValidator() {		
	}
	
	public boolean isPropertyMissing(Input input) {
		return !getMissingProperties(input).isEmpty();
	}
	
	public Set<URI> getMissingProperties(Input input) {
		properties = Sets.newHashSet();
		input.accept(this);
		return properties;
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(AtomicInput input) {
	    hasValue = (input.getValue() != null);
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(IntersectionInput intersection) {
    	for (Input input : intersection.getInputs()) {
	        input.accept(this);
        }
    	hasValue = true;
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(UnionInput union) {
    	int valueIndex = union.getValueIndex();
    	if (valueIndex != -1) {
    		Input input = union.getInputs().get(valueIndex);
    		input.accept(this);
    	}
	    hasValue = (valueIndex != 1);
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(EnumeratedInput input) {
    	hasValue = (input.getValue() != null);
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(PropertyInput input) {
    	input.getRange().accept(this);
    	if (!hasValue && input.getMinCardinality() > 0) {
    		properties.add(input.getProperty());
    	}
    	hasValue = true;
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(URIValue value) {
	    // do nothing	    
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(BNodeValue value) {
	    // do nothing	    
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void visit(LiteralValue value) {
	    // do nothing	    
    }

}
