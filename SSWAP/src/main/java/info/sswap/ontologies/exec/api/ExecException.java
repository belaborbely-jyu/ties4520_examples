/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.api;

/**
 * Root class for all Exec-specific exceptions.
 * 
 * @see Exec
 * 
 * @author Damian Gessler
 *
 */
public class ExecException extends Exception {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception
	 */
	public ExecException() {		
	}
	
	/**
	 * Creates a new exception with the specified message
	 * 
	 * @param message the specified message
	 */
	public ExecException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new exception with the specified underlying cause
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public ExecException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new exception with the specified message and the underlying cause
	 * @param message the message describing the exception
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public ExecException(String message, Throwable cause) {
		super(message, cause);
	}
}
