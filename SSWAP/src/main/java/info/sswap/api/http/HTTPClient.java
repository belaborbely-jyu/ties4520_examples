/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.http;

import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;

import java.io.IOException;
import java.io.InputStream;

/**
 * Access the HTTP API within the Java API. Useful for sending JSON data to the
 * HTTP API to make SSWAP models such as:
 * <ul>
 * <li><code>RIG</code>: <a href="http://sswap.info/api/makeRIG">Resource Invocation Graph</a>
 * <li><code>RQG</code>: <a href="http://sswap.info/api/makeRQG">Resource Query Graph</a>
 * </ul>
 * <p>
 * To create an <code>HTTPClient</code>, use <code>HTTPAPI.getClient()</code>.
 * <p>
 * Note: Implementation may make a runtime network call to
 * <a href="http://sswap.info/api">http://sswap.info/api</a> to perform conversions.
 * 
 * @see info.sswap.api.model.RIG
 * @see info.sswap.api.model.RQG
 * @see HTTPProvider
 * @see info.sswap.api.spi.HTTPAPI
 * 
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * 
 */
public interface HTTPClient {
	
	/**
	 * Make a Response Invocation Graph (<code>RIG</code>) from a JSON
	 * (JavaScript Object Notation) specification. For the specification, see
	 * <a href="http://sswap.info/api/makeRIG">http://sswap.info/api/makeRIG</a>.
	 * 
	 * @param jsonStream
	 *            a stream, for example from a file, of JSON input
	 * @return a compound object that allows one to get the RIG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see info.sswap.api.model.RIG
	 * @see RIGResponse#getRIG
	 */
	public RIGResponse makeRIG(InputStream jsonStream) throws IOException;
	
	/**
	 * Convenience method to <code>makeRIG(InputStream)</code> with input as
	 * single JSON string.
	 * 
	 * @param jsonString
	 *            a string, for example from local variables and validated user
	 *            input, of JSON input
	 * @return a compound object that allows one to get the RIG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see #makeRIG(InputStream)
	 */
	public RIGResponse makeRIG(String jsonString) throws IOException;
	
	/**
	 * Make a Response Query Graph (<code>RQG</code>) from a JSON (JavaScript
	 * Object Notation) specification. For the specification, see
	 * <a href="http://sswap.info/api/makeRQG">http://sswap.info/api/makeRQG</a>.
	 * 
	 * @param jsonStream
	 *            a stream, for example from a file, of JSON input
	 * @return a compound object that allows one to get the RQG on success, or
	 *         examine the connection response on error
	 * @see info.sswap.api.model.RQG
	 * @throws IOException on network or parsing error
	 * @see RQGResponse#getRQG
	 */
	public RQGResponse makeRQG(InputStream jsonStream) throws IOException;
	
	/**
	 * Convenience method to <code>makeRQG(InputStream)</code> with input as
	 * single JSON string.
	 * 
	 * @param jsonString
	 *            a string, for example from local variables and validated user
	 *            input, of JSON input
	 * @return a compound object that allows one to get the RQG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see #makeRQG(InputStream)
	 */
	public RQGResponse makeRQG(String jsonString) throws IOException;
	
	/**
	 * Access to get an <code>RIG</code> on success or a network response object
	 * (via the superinterface) on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public interface RIGResponse extends HTTPResponse {

		/**
		 * Getter method to get the <code>RIG</code> from a successful conversion.
		 * @return successful <code>RIG</code> or null on failure
		 */
		public RIG getRIG();
		
	}
	
	/**
	 * Access to get an <code>RQG</code> on success or a network response object
	 * (via the superinterface) on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public interface RQGResponse extends HTTPResponse {

		/**
		 * Getter method to get the <code>RQG</code> from a successful conversion.
		 * 
		 * @return successful <code>RQG</code> or null on failure
		 */
		public RQG getRQG();

	}
	
}
