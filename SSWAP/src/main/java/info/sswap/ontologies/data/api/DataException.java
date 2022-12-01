/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

import info.sswap.api.model.DataAccessException;

/**
 * Root class for all Data-specific exceptions.
 * 
 * @see Data
 * 
 * @author Damian Gessler
 *
 */
public class DataException extends DataAccessException {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception
	 */
	public DataException() {		
	}
	
	/**
	 * Creates a new exception with the specified message
	 * 
	 * @param message the specified message
	 */
	public DataException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new exception with the specified underlying cause
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public DataException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new exception with the specified message and the underlying cause
	 * @param message the message describing the exception
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public DataException(String message, Throwable cause) {
		super(message, cause);
	}
}
