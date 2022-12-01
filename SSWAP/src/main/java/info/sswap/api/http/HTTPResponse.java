/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Response information from a HTTP network call.  Based on the Response
 * class of <code>com.clarkparsia.utils.web.Response</code>.
 */
public interface HTTPResponse {
	
	/**
	 * Return the error stream from the connection
	 * 
	 * @return the error stream
	 */
	public InputStream getErrorStream();

	/**
     * Return the response message from the server
     * 
     * @return the message
     */
	public String getMessage();

	/**
	 * Return all headers returned by the server
	 * 
	 * @return a Map of each header name and a List of its values
	 */
	public Collection<String> getHeaders();
	
	/**
	 * Returns the unparsed value of the HTTP header
	 * @param headerName the header name
	 * 
	 * @return the header value; null if a header for <code>headerName</code>
	 *         does not exist
	 */
	public String getHeaderValue(String headerName);
	
	/**
	 * Returns the values of the HTTP header parsed on the comma delimiter
	 * @param headerName the header name
	 * 
	 * @return the header values; null if a header for <code>headerName</code>
	 *         does not exist
	 */
	public Collection<String> getHeaderValues(String headerName);
	
	/**
	 * Returns values, if any, for the SSWAP Exception Header(s). If
	 * there are no exceptions the collection will be empty, but not null.
	 * 
	 * @return SSWAP over HTTP Exception messages
	 * @see #getSSWAPExceptionHeader()
	 */
	public Collection<String> getSSWAPExceptionValues();
	
	/**
	 * Returns the header being used for SSWAP Exceptions.
	 * 
	 * @return SSWAP Exception header
	 * @see #getSSWAPExceptionValues()
	 */
	public String getSSWAPExceptionHeader();
	
	/**
	 * Access to an underlying exception that may have been thrown
	 * upon failure to generate a response.
	 * 
	 * @return exception; may be null
	 */
	public Exception getException();
	
    /**
     * Return the response code
     * 
     * @return the response code
     */
	public int getResponseCode();

    /**
     * Return the response content from the server
     * 
     * @return the response content
     */
	public InputStream getContent();

    /**
     * Return whether or not this has an error result
     * 
     * @return true if there is an error result, false otherwise
     */
	public boolean hasErrorCode();
	
	/**
     * Close this response
     * @throws IOException if there is an error while closing
     */
	public void close() throws IOException;
	
}
