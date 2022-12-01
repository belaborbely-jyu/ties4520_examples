/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.Input;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.IntersectionInput;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * @author Evren Sirin
 */
public class IntersectionInputImpl extends AbstractInput implements IntersectionInput {
	private final ImmutableList<Input> inputs;
	
	public IntersectionInputImpl(Collection<Input> inputs) {
		super(URI.create(OWL.intersectionOf.getURI()));
		
		this.inputs = ImmutableList.copyOf(inputs);	
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
		return 31 * inputs.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IntersectionInput))
			return false;

		IntersectionInput that = (IntersectionInput) obj;
		return this.genericEqualTo(that)
		       && this.inputs.size() == that.getInputs().size()
		       && Iterators.elementsEqual(this.inputs.iterator(), that.getInputs().iterator());
	}

	@Override
    public List<Input> getInputs() {
	    return inputs;
    }
	
	@Override
	public String toString() {
		return "[Intersection: " + Joiner.on(", ").join(inputs) + "]";
	}
}
