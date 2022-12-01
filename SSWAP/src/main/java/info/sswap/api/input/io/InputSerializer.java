/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.Input;

/**
 * Interface for serializing {@link Input} values to generic objects.
 * 
 * @author Evren Sirin
 */
public interface InputSerializer<T> {
	/**
	 * Serializes the given input to an object.
	 */
	public T serialize(Input input);
}
