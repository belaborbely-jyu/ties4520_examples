/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import java.net.HttpURLConnection;

/**
 * Exception class for HTTP response code 400 Bad Request.
 * 
 * @author Damian Gessler
 * 
 */
public class ClientException extends RuntimeException {

	protected static String defaultMessage = "Bad request: specification for the data is ambiguous or otherwise inadequately specified";
	
	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception
	 */
	public ClientException() {
		super(defaultMessage);
	}
	
	// may not able to pass a user message back through AbstractSSWAPServlet.sendError() and
	// RIGImpl.doInvoke() when invoked with a GET
	
	/**
	 * Creates a new exception with the detail message
	 * 
	 * @param detailMessage exception detail message
	 */
	public ClientException(String detailMessage) {
		super(detailMessage);
	}
	
	/**
	 * Creates a new exception with the exception
	 * @param exception source exception for this client exception
	 */
	public ClientException(Exception exception) {
		super(exception);
	}
	
	/**
	 * The HTTP response (status) code 400 Request Entity Too Large
	 * @return response code 400
	 */
	public int getResponseCode() {
		return HttpURLConnection.HTTP_BAD_REQUEST;
	}
	
}
