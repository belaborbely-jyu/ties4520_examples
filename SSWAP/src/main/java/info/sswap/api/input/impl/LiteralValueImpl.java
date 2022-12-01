/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.LiteralValue;

import java.io.Serializable;
import java.net.URI;

import org.mindswap.pellet.utils.URIUtils;

import com.google.common.base.Objects;

/**
 * @author Evren Sirin
 */
public class LiteralValueImpl implements LiteralValue, Serializable {
	private final String label;
	private final String language;
	private final URI datatype;

	public LiteralValueImpl(String label) {
		this.label = label;
		this.language = null;
		this.datatype = null;
	}

	public LiteralValueImpl(String label, String language) {
		this.label = label;
		this.language = language;
		this.datatype = null;
	}

	public LiteralValueImpl(String label, URI datatype) {
		this.label = label;
		this.language = null;
		this.datatype = datatype;
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
		return 31 * label.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LiteralValue))
			return false;

		LiteralValue that = (LiteralValue) obj;
		return this.label.equals(that.getLabel())
		       && Objects.equal(this.language, that.getLanguage())
		       && Objects.equal(this.datatype, that.getDatatype());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLabel() {
		return label;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLanguage() {
		return language;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public URI getDatatype() {
		return datatype;
	}

	@Override
	public String toString() {
		return "\""
		       + label
		       + "\""
		       + (language != null ? "@" + language : datatype != null ? "^^" + URIUtils.getLocalName(datatype) : "");
	}
}
