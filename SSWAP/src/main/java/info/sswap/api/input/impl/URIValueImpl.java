/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.URIValue;

import java.io.Serializable;
import java.net.URI;

/**
 * @author Evren Sirin
 */
public class URIValueImpl implements URIValue, Serializable {
	private final URI uri;
	
	public URIValueImpl(URI uri) {
	    this.uri = uri;
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
	    return 31 * uri.hashCode();
    }

	@Override
    public boolean equals(Object obj) {
		if (!(obj instanceof URIValue))
			return false;
		
		URIValue that = (URIValue) obj;
		return this.uri.equals(that.getURI());
    }

	@Override
    public URI getURI() {
	    return uri;
    }
	
	@Override
	public String toString() {
		return "<" + uri + ">";
	}
}
