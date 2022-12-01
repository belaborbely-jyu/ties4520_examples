/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * An exception that is thrown when a validation fails in SSWAP (i.e., it is found during the validation that the
 * underlying RDF data does not conform to the SSWAP syntax or requirements.)
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public class ValidationException extends Exception {
	/**
	 * A serial version id for a serializable class.
	 */
	private static final long serialVersionUID = -7021644823589935053L;

	/**
	 * Creates an empty ValidationException with no message.
	 */
	public ValidationException() {
	}

	/**
	 * Creates a ValidationException with a message.
	 * 
	 * @param message
	 *            the message describing why the validation failed.
	 */
	public ValidationException(String message) {
		super(message);
	}

	/**
	 * Creates a ValidationException with an underlying exception that caused this exception to be generated.
	 * 
	 * @param cause
	 *            the underlying exception
	 */
	public ValidationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a ValidationException with both a message and an underlying exception that caused this exception to be
	 * generated.
	 * 
	 * @param message
	 *            the message describing why the validation failed
	 * @param cause
	 *            the underlying exception
	 */
	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
