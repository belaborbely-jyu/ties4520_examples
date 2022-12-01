/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * A generic, unchecked exception thrown when a problem with accessing the underlying data should occur (e.g.,
 * because the data source is unavailable or the data cannot be parsed).
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public class DataAccessException extends RuntimeException {
	/**
     * A serial version id for a serializable class.
     */
    private static final long serialVersionUID = -4049689824313517768L;

	/**
	 * Creates a new exception
	 */
	public DataAccessException() {		
	}
	
	/**
	 * Creates a new exception with the specified message
	 * 
	 * @param message the specified message
	 */
	public DataAccessException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new exception with the specified underlying cause
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public DataAccessException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new exception with the specified message and the underlying cause
	 * @param message the message describing the exception
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public DataAccessException(String message, Throwable cause) {
		super(message, cause);
	}
}
