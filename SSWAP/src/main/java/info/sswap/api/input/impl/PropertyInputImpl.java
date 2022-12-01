/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.Input;
import info.sswap.api.input.InputFactory;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.PropertyInput;

import java.net.URI;

import org.mindswap.pellet.utils.URIUtils;

import com.hp.hpl.jena.vocabulary.OWL;

/**
 * @author Evren Sirin
 */
public class PropertyInputImpl extends AbstractInput implements PropertyInput {
	private final URI property;
	private Input range;
	private int minCardinality;
	private int maxCardinality;

	public PropertyInputImpl(URI property) {
		super(URI.create(OWL.Restriction.getURI()));

		setLabel(URIUtils.getLocalName(property));

		this.property = property;
		this.range = InputFactory.createUnrestricedInput();
		this.minCardinality = 0;
		this.maxCardinality = Integer.MAX_VALUE;
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
		return 31 * property.hashCode() * range.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PropertyInput))
			return false;

		PropertyInput that = (PropertyInput) obj;
		return this.genericEqualTo(that)
		       && this.property.equals(that.getProperty())
		       && this.minCardinality == that.getMinCardinality()
		       && this.maxCardinality == that.getMaxCardinality()
		       && this.range.equals(that.getRange());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public URI getProperty() {
		return property;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Input getRange() {
		return range;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRange(Input range) {
		this.range = range;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMinCardinality() {
		return minCardinality;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasMinCardinality() {
		return minCardinality != DEFAULT_MIN;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMinCardinality(int minCardinality) {
		this.minCardinality = minCardinality;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMaxCardinality() {
		return maxCardinality;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasMaxCardinality() {
		return maxCardinality != DEFAULT_MAX;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMaxCardinality(int maxCardinality) {
		this.maxCardinality = maxCardinality;
	}

	@Override
	public String toString() {
		return "[Property: "
		       + toStringLabel()
		       + "("
		       + (hasMinCardinality() ? minCardinality : "_")
		       + ","
		       + (hasMaxCardinality() ? maxCardinality : "_")
		       + ") "
		       + range
		       + "]";
	}

}
