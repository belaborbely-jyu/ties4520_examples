/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.spi;

import info.sswap.api.http.HTTPClient;
import info.sswap.api.http.HTTPProvider;
import info.sswap.impl.http.HTTPAPIImpl;

/**
 * Accessor class to get <code>HTTPClient</code> and <code>HTTPProvider</code>s.
 * 
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * @see info.sswap.api.http.HTTPClient
 * @see info.sswap.api.http.HTTPProvider
 *
 */
public class HTTPAPI {
	/**
	 * Getter for a new Client. Allows Java API access to HTTP API graph generation.
	 * 
	 * @return a new Client
	 * @see info.sswap.api.http.HTTPClient
	 */
	public static HTTPClient getClient() {
		return new HTTPAPIImpl();
	}
	
	/**
	 * Getter for a new Provider.  Allows Java API access to HTTP API graph generation.
	 * 
	 * @return a new Provider
     * @see info.sswap.api.http.HTTPProvider
	 */
	public static HTTPProvider getProvider() {
		return new HTTPAPIImpl();
	}
}
