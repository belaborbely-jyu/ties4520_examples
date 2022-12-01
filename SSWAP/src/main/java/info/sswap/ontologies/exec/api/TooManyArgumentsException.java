/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.api;

/**
 * Exception class for user arguments as parsed from <code>exec:args</code>
 * predicate exceeding a pre-set maximum number.
 * 
 * @author Damian Gessler
 * 
 */
public class TooManyArgumentsException extends ExecException {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception
	 */
	public TooManyArgumentsException() {		
	}
	
	/**
	 * Creates a new exception with the specified message
	 * 
	 * @param message the specified message
	 */
	public TooManyArgumentsException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new exception with the specified underlying cause
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public TooManyArgumentsException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new exception with the specified message and the underlying cause
	 * @param message the message describing the exception
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public TooManyArgumentsException(String message, Throwable cause) {
		super(message, cause);
	}
}
