/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

import java.io.IOException;
import java.net.URLConnection;

/**
 * Interface to web resources that assist or enable accessing a web data source
 * or destination (e.g, username and password authentication and authorization).
 * 
 * @author Damian Gessler
 * 
 */
public interface Accessor {

	/**
	 * Method acts as a filter: it is passed a URLConnection and should
	 * return an authenticated URLConnection suitable for immediate reading.
	 * 
	 * @param urlConnection URL connection to be opened (authenticated, authorized, etc.)
	 * @return an authenticated, authorized, etc. URL connection ready for reading
	 * @throws IOException on any connection error
	 * @throws AccessorException on any Accessor-specific error
	 */
	public URLConnection access(URLConnection urlConnection) throws IOException, AccessorException;
	
}
