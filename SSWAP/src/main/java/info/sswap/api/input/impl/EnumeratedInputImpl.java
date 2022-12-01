/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.InputValue;
import info.sswap.api.input.InputVisitor;

import java.net.URI;
import java.util.Collection;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * @author Evren Sirin
 */
public class EnumeratedInputImpl extends AbstractInput implements EnumeratedInput {
	private final ImmutableSet<InputValue> values;

	public EnumeratedInputImpl(Collection<InputValue> values) {
		super(URI.create(OWL.oneOf.getURI()));

		this.values = ImmutableSet.copyOf(values);
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
		return 31 * values.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EnumeratedInput))
			return false;

		EnumeratedInput that = (EnumeratedInput) obj;
		return this.genericEqualTo(that)
		       && this.values.size() == that.getValues().size()
		       && this.values.equals(that.getValues());
	}

	@Override
	public Collection<InputValue> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return "[EnumeratedInput: " + Joiner.on(", ").join(values) + "]";
	}
}
