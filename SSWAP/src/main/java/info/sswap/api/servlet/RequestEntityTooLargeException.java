/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import java.net.HttpURLConnection;

/**
 * Exception class for HTTP response code 413 Request Entity Too Large.
 * 
 * @author Damian Gessler
 * 
 */
public class RequestEntityTooLargeException extends ClientException {
	
	protected static String defaultMessage = "Request Entity Too Large: the amount of data or the complexity of the mapping is too large or too complex";

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The HTTP response (status) code 413 Request Entity Too Large
	 * @return response code 413
	 */
	public int getResponseCode() {
		return HttpURLConnection.HTTP_ENTITY_TOO_LARGE;
	}
	
}
