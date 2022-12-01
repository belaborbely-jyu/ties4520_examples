/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */

package info.sswap.api.input;

import java.net.URI;

/**
 * Represents an atomic input type for named classes or datatypes. This input type is typically used for classes that do
 * not define any further restrictions though the datatype URI might have built-in restrictions. For example, the type
 * <code>xsd:integer</code> indicates that the input value should be a valid integer value.
 * 
 * @author Evren Sirin
 */
public interface AtomicInput extends Input {
	/**
	 * Returns the type of the named class or datatype. 
	 */
	public URI getType();	
}
