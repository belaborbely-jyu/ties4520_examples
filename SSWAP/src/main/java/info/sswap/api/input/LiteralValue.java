/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;

/**
 * Represents a literal value that might optionally have a language tag or a datatype URI but not both.
 * 
 * @author Evren Sirin
 */
public interface LiteralValue extends InputValue {
	/**
	 * Returns the lexical form of the literal. 
	 */
	public String getLabel();
	
	/**
	 * Returns the language tag of the literal or <code>null</code> if there is none. 
	 */
	public String getLanguage();
	
	/**
	 * Returns the datatype URI of the literal or <code>null</code> if there is none. 
	 */
	public URI getDatatype();	
}
