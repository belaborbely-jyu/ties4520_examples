/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

/**
 * Parser-specific exceptions.
 * 
 * @see Parser
 * 
 * @author Damian Gessler
 *
 */
public class ParserException extends DataException {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception
	 */
	public ParserException() {		
	}
	
	/**
	 * Creates a new exception with the specified message
	 * 
	 * @param message the specified message
	 */
	public ParserException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new exception with the specified underlying cause
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public ParserException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new exception with the specified message and the underlying cause
	 * @param message the message describing the exception
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public ParserException(String message, Throwable cause) {
		super(message, cause);
	}
}
