/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.http;

import info.sswap.api.model.PDG;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RRG;

import java.io.IOException;
import java.io.InputStream;

/**
 * Access the HTTP API within the Java API. Useful for sending JSON data to the
 * HTTP API to make SSWAP models such as:
 * <ul>
 * <li><code>PDG</code>: <a href="http://sswap.info/api/makePDG">Provider Description Graph</a>
 * <li><code>RDG</code>: <a href="http://sswap.info/api/makeRDG">Resource Description Graph</a>
 * <li><code>RRG</code>: <a href="http://sswap.info/api/makeRRG">Resource Response Graph</a>
 * </ul>
 * <p>
 * To create an <code>HTTPProvider</code>, use <code>HTTPAPI.getProvider()</code>.
 * <p>
 * Note: Implementation may make a runtime network call to <a
 * href="http://sswap.info/api">http://sswap.info/api</a> to perform
 * conversions.
 * 
 * @see info.sswap.api.model.PDG
 * @see info.sswap.api.model.RDG
 * @see info.sswap.api.model.RRG
 * @see HTTPClient
 * @see info.sswap.api.spi.HTTPAPI
 * 
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * 
 */
public interface HTTPProvider {
	
	/**
	 * Make a Provider Description Graph (<code>PDG</code>) from a JSON
	 * (JavaScript Object Notation) specification. For the specification, see
	 * <a href="http://sswap.info/api/makePDG">http://sswap.info/api/makePDG</a>.
	 * 
	 * @param jsonStream
	 *            a stream, for example from a file, of JSON input
	 * @return a compound object that allows one to get the PDG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see info.sswap.api.model.PDG
	 * @see PDGResponse#getPDG
	 */
	public PDGResponse makePDG(InputStream jsonStream) throws IOException;
	
	/**
	 * Convenience method to <code>makePDG(InputStream)</code> with input as
	 * single JSON string.
	 * 
	 * @param jsonString
	 *            a string, for example from local variables and validated user
	 *            input, of JSON input
	 * @return a compound object that allows one to get the PDG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see #makePDG(InputStream)
	 */
	public PDGResponse makePDG(String jsonString) throws IOException;
	
	/**
	 * Make a Resource Description Graph (<code>RDG</code>) from a JSON
	 * (JavaScript Object Notation) specification. For the specification, see
	 * <a href="http://sswap.info/api/makeRDG">http://sswap.info/api/makeRDG</a>.
	 * 
	 * @param jsonStream
	 *            a stream, for example from a file, of JSON input
	 * @return a compound object that allows one to get the RDG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see info.sswap.api.model.RDG
	 * @see RDGResponse#getRDG
	 */
	public RDGResponse makeRDG(InputStream jsonStream) throws IOException;
	
	/**
	 * Convenience method to <code>makeRDG(InputStream)</code> with input as
	 * single JSON string.
	 * 
	 * @param jsonString
	 *            a string, for example from local variables and validated user
	 *            input, of JSON input
	 * @return a compound object that allows one to get the RDG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see #makeRDG(InputStream)
	 */
	public RDGResponse makeRDG(String jsonString) throws IOException;

	/**
	 * Make a Resource Response Graph (<code>RRG</code>) from a JSON
	 * (JavaScript Object Notation) specification. For the specification, see
	 * <a href="http://sswap.info/api/makeRRG">http://sswap.info/api/makeRRG</a>.
	 * 
	 * @param jsonStream
	 *            a stream, for example from a file, of JSON input
	 * @return a compound object that allows one to get the RRG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see info.sswap.api.model.RRG
	 * @see RRGResponse#getRRG
	 */
	public RRGResponse makeRRG(InputStream jsonStream) throws IOException;
	
	/**
	 * Convenience method to <code>makeRRG(InputStream)</code> with input as
	 * single JSON string.
	 * 
	 * @param jsonString
	 *            a string, for example from local variables and validated user
	 *            input, of JSON input
	 * @return a compound object that allows one to get the RRG on success, or
	 *         examine the connection response on error
	 * @throws IOException on network or parsing error
	 * @see #makeRRG(InputStream)
	 */
	public RRGResponse makeRRG(String jsonString) throws IOException;

	/**
	 * Exposes access to both a ready-to-use Provider Description Graph
	 * (<code>PDG</code>) on success or a network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public interface PDGResponse extends HTTPResponse {
		
		/**
		 * Getter method to get the Provider Description Graph (<code>PDG</code>) from a successful conversion.
		 * @return successful <code>PDG</code> or null on failure.
	     * @see info.sswap.api.model.PDG
		 */
		public PDG getPDG();

	}
	
	/**
	 * Exposes access to both a ready-to-use Resource Description Graph (<code>RDG</code>) on success or a
	 * network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public interface RDGResponse extends HTTPResponse {
		
		/**
		 * Getter method to get the Resource Description Graph (<code>RDG</code>) from a successful conversion.
		 * @return successful <code>RDG</code> or null on failure.
	     * @see info.sswap.api.model.RDG
		 */
		public RDG getRDG();

	}
	
	/**
	 * Exposes access to both a ready-to-use Resource Response Graph (<code>RRG</code>) on success or a
	 * network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public interface RRGResponse extends HTTPResponse {
	
		/**
		 * Getter method to get the Resource Response Graph (<code>RRG</code>) from a successful conversion.
		 * @return successful <code>RRG</code> or null on failure.
		 * @see info.sswap.api.model.RRG
		 */
		public RRG getRRG();

	}
	
}
