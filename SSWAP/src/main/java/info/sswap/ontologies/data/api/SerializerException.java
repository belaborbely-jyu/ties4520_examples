/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

/**
 * Serializer-specific exceptions.
 * 
 * @see Serializer
 * 
 * @author Damian Gessler
 *
 */
public class SerializerException extends DataException {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception
	 */
	public SerializerException() {		
	}
	
	/**
	 * Creates a new exception with the specified message
	 * 
	 * @param message the specified message
	 */
	public SerializerException(String message) {
		super(message);
	}
	
	/**
	 * Creates a new exception with the specified underlying cause
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public SerializerException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new exception with the specified message and the underlying cause
	 * @param message the message describing the exception
	 * @param cause the underlying exception that caused this exception to be generated
	 */
	public SerializerException(String message, Throwable cause) {
		super(message, cause);
	}
}
