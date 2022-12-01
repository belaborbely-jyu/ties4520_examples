/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.AtomicInput;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.Vocabulary;

import java.net.URI;

import org.mindswap.pellet.utils.URIUtils;

/**
 * @author Evren Sirin
 */
public class AtomicInputImpl extends AbstractInput implements AtomicInput {
	public AtomicInputImpl() {
		super(Vocabulary.OWL_THING);
		
		setLabel("Unrestricted");
	}
	
	public AtomicInputImpl(URI type) {
		super(type);
		
		if (isUnrestricted())
			setLabel("Unrestricted");
		else
			setLabel(URIUtils.getLocalName(type));
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    public void accept(InputVisitor visitor) {
	    visitor.visit(this);
    }
    
	@Override
    public int hashCode() {
	    return 31 * type.hashCode();
    }

	@Override
    public boolean equals(Object obj) {
		if (!(obj instanceof AtomicInput))
			return false;
		
		AtomicInput that = (AtomicInput) obj;
		return this.genericEqualTo(that) && this.type.equals(that.getType());
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isUnrestricted() {
		return type.equals(Vocabulary.OWL_THING) || type.equals(Vocabulary.RDFS_LITERAL);
	}
	
	@Override
	public String toString() {
		return "[Atomic: " + toStringLabel() + "]";
	}
}
