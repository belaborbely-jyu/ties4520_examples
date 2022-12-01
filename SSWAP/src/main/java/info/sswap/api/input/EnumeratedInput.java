/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;
import java.util.Collection;

/**
 * Represents an enumerated input where the allowed values should be chosen from a given set of values.
 * 
 * @author Evren Sirin
 */
public interface EnumeratedInput extends Input {
	/**
	 * Returns <code>owl:oneOf</code>
	 */
	public URI getType();
	
	/**
	 * Returns the allowed set of input values for this input.
	 */
	public Collection<InputValue> getValues();
}
