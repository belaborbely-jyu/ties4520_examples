/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.Input;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.UnionInput;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * @author Evren Sirin
 */
public class UnionInputImpl extends AbstractInput implements UnionInput {
	private final ImmutableList<Input> inputs;
	private final URI[] valueTypes;
	private int valueIndex = -1;

	public UnionInputImpl(Collection<Input> inputs) {
		super(URI.create(OWL.unionOf.getURI()));

		this.inputs = ImmutableList.copyOf(inputs);
		this.valueTypes = new URI[inputs.size()];
		Arrays.fill(valueTypes, null);
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
		if (!(obj instanceof UnionInput))
			return false;

		UnionInput that = (UnionInput) obj;
		return this.genericEqualTo(that)
		       && this.valueIndex == that.getValueIndex()
		       && this.inputs.size() == that.getInputs().size()
		       && Iterators.elementsEqual(this.inputs.iterator(), that.getInputs().iterator())
		       && Arrays.equals(this.valueTypes, ((UnionInputImpl) that).valueTypes);
	}

	@Override
	public List<Input> getInputs() {
		return inputs;
	}

	@Override
	public int getValueIndex() {
		return valueIndex;
	}

	@Override
	public void setValueIndex(int valueIndex) {
		if (valueIndex < -1 || valueIndex >= inputs.size())
			throw new IndexOutOfBoundsException("Index: "
			                                    + valueIndex
			                                    + " is not in the range [-1,"
			                                    + inputs.size()
			                                    + "]");
		this.valueIndex = valueIndex;
	}

	@Override
	public URI getValueType(int valueIndex) {
		return valueTypes[valueIndex];
	}

	@Override
	public void setValueType(int valueIndex, URI valueType) {
		valueTypes[valueIndex] = valueType;
	}

	@Override
	public String toString() {
		return "[Union: " + Joiner.on(", ").join(inputs) + "]";
	}
}
