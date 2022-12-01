/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;


/**
 * Represent a <code>URI</code> value by wrapping a {@link URI} object. 
 * 
 * @author Evren Sirin
 */
public interface URIValue extends InputValue {
	/**
	 * Returns the URI value.
	 */
	public URI getURI();	
}
