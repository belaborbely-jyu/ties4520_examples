/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.api.model.DataAccessException;
import info.sswap.ontologies.data.api.Accessor;
import info.sswap.ontologies.data.api.AccessorException;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;

/**
 * Implementation of a HTTP Basic Authentication Accessor
 * 
 * @author Damian Gessler
 *
 */
public class HTTPBasicAuthenticationAccessor implements Accessor {

	/**
	 * A URN for designating HTTP Basic Authentication
	 */
	public final static URI uri = URI.create("urn:sswap:accessor:httpBasicAuthentication");
	
	private String encodedUsernameAndPassword;

	/**
	 * HTTP Basic Authentication Accessor
	 * 
	 * @param username the username (login) to authenticate
	 * @param passwd the password for the username
	 * @throws DataAccessException on any error in establishing access
	 */
	public HTTPBasicAuthenticationAccessor(String username, String passwd) throws DataAccessException {
		// encodedUsernameAndPassword = Base64.encodeBase64String((username + ":" + passwd).getBytes());    // commons codec 1.5
		encodedUsernameAndPassword = new String(Base64.encodeBase64((username + ":" + passwd).getBytes())); // commons codec 1.3 complaint
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public URLConnection access(URLConnection urlConnection) throws IOException, AccessorException {
		
		urlConnection.setRequestProperty("Authorization", "Basic " + encodedUsernameAndPassword);
		
		return urlConnection;
	}

}
