/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.util.List;


/**
 * Represents an input type that is a combination of other inputs.
 * 
 * @author Evren Sirin
 */
public interface NaryInput extends Input {
	public List<Input> getInputs();
}
