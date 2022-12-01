/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.ontologies.data.api.AccessorException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Implementation of support for HTTP Authentication for resource data
 * 
 * @author Damian Gessler
 *
 */
public class HTTPBasicAuthImpl extends ResourceData {

	private String username = null;
	private String passwd = null;
	
	/**
	 * Construct HTTP Authentication access.
	 * 
	 * @param sswapIndividual the resource data to be read/written requiring HTTP Basic Authentication
	 * @param username username to be authenticated
	 * @param passwd password for the username
	 * @throws DataAccessException on any error establishing access
	 */
	public HTTPBasicAuthImpl(SSWAPIndividual sswapIndividual, String username, String passwd) throws DataAccessException {
		
		super(sswapIndividual);
		
		if ( username == null || passwd == null ) {
			throw new DataAccessException("Must specify a username and password");
		}
		
		this.username = username;
		this.passwd = passwd;
				
	}

	/**
	 * If the individual has a <code>data:hasAccessor</code> property value
	 * equal to the value of <code>HTTPBasicAuthenticationAccessor.uri</code>,
	 * then a HTTP Basic Authentication connection is opened. Otherwise, a
	 * regular connection is opened.
	 * 
	 * @return URLConnection an open URL connection
	 * @throws IOException
	 *             on network error
	 * @throws AccessorException
	 *             on access error
	 * 
	 * @see HTTPBasicAuthenticationAccessor
	 */
	@Override
	public URLConnection access(URLConnection urlConnection) throws IOException, AccessorException {

		if ( urlConnection instanceof HttpURLConnection ) {
			
			HttpURLConnection httpURLConnection = null;
			
			try {
				
				httpURLConnection = (HttpURLConnection) urlConnection;
				httpURLConnection.connect();
				
				int responseCode = httpURLConnection.getResponseCode();
				if ( responseCode == HttpURLConnection.HTTP_UNAUTHORIZED ) { // 401
					httpURLConnection.disconnect();
					urlConnection = (new HTTPBasicAuthenticationAccessor(username,passwd)).access(urlConnection);
				} else if ( responseCode >= 400 ) {
					throw new IOException(httpURLConnection.getResponseMessage());
				}
			} catch ( IOException ioe ) {
				throw ioe;
			} catch ( Exception e ) {
				throw new AccessorException(e.getMessage());
			}
			
		} else {
			urlConnection = super.access(urlConnection);
		}
		
		return urlConnection;

	}
	
	/**
	 * The username for authentication
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Set the username and password; replaces username and password as set with the constructor.
	 * 
	 * @param username username to be authenticated
	 * @param passwd password for username
	 */
	public void setUsernamePasswd(String username, String passwd) {
		this.username = username;
		this.passwd = passwd;
	}
	
	/**
	 * The password for the username
	 * @return the password
	 */
	public String getPasswd() {
		return passwd;
	}

}
