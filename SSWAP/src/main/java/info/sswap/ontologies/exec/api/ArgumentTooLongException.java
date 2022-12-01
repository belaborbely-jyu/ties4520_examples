/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.api;

/**
 * Exception class for a user argument as parsed from <code>exec:args</code>
 * predicate exceeding a pre-set maximum length.
 * 
 * @author Damian Gessler
 * 
 */
public class ArgumentTooLongException extends ExecException {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception
	 */
	public ArgumentTooLongException() {		
	}
	
	/**
	 * Creates a new exception with the specified message
	 * 
	 * @param message the specified message
	 */
	public ArgumentTooLongException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new exception with the specified underlying cause
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public ArgumentTooLongException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new exception with the specified message and the underlying cause
	 * @param message the message describing the exception
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public ArgumentTooLongException(String message, Throwable cause) {
		super(message, cause);
	}
}
