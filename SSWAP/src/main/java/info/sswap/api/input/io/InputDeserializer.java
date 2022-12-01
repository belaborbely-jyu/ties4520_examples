/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.Input;

/**
 * Interface for deserializing {@link Input} from generic objects.
 * 
 * @author Evren Sirin
 */
public interface InputDeserializer<T> {
	/**
	 * Deserializes an {@link Input} instance from the given object.
	 */
	public Input deserialize(T obj);
}
