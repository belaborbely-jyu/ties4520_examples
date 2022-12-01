/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.api.model.SSWAPIndividual;
import info.sswap.ontologies.data.api.Accessor;
import info.sswap.ontologies.data.api.AccessorException;
import info.sswap.ontologies.data.api.DataException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Set;


import com.google.common.io.ByteStreams;

/**
 * Support for reading and writing resource data (data at the URL of an
 * individual; usually a <code>SSWAPSubject</code> or <code>SSWAPObject</code>).
 * 
 * @author Damian Gessler
 * 
 */
public class ResourceData extends AbstractData implements Accessor {

	protected URLConnection urlConnection = null;
	
	/**
	 * Constructs an individual suitable for reading/writing data
	 * 
	 * @param sswapIndividual the subject data individual
	 * @throws DataException on any error establishing resource data support
	 */
	public ResourceData(SSWAPIndividual sswapIndividual) throws DataException {
		super(sswapIndividual);
	}

	/**
	 * Accesses, parses, and validates the data from the URL of the data
	 * individual. Order of processing is to call the <code>access</code>,
	 * <code>parse</code>, and <code>validate</code> methods, any of which may
	 * be overridden.
	 * <p>
	 * The data source must be of type <code>data:DataFormat</code> for any
	 * meaningful read; otherwise a read immediately "succeeds" with trivial
	 * empty content.
	 * 
	 * @throws IOException
	 *             on any read error
	 * @throws DataException
	 *             on data error such as parsing, validating, etc.
	 */
	@Override
	public InputStream readData() throws IOException, DataException {
		
		URL url;
		
		if ( ! sswapIndividual.isOfType(DataFormatType) ) {
			return new ByteArrayInputStream(new byte[0]); // null read
		} else {
			try {
				url = getURLConnection();
			} catch ( Exception e ) {
				throw new AccessorException("read error: " + e.getMessage());
			}
		}
		
		urlConnection = access(url.openConnection());
		
		return validate(parse(urlConnection.getInputStream()));
	}

	/**
	 * Accesses, validates, and serializes the data to the URL of the data
	 * individual. Order of processing is to call the <code>access</code>,
	 * <code>validate</code>, and <code>serialize</code> methods, any of which
	 * may be overridden. Caller should close inputStream when done; opened URL
	 * connection will be closed automatically on success only. On failure
	 * (<i>e.g.</i>, a thrown exception), use <code>getConnection</code> to get
	 * the underlying connection for error handling. Connection should be then
	 * closed by caller when done.
	 * 
	 * @param inputStream
	 *            data to be written
	 * @throws IOException
	 *             on any write error
	 * @throws DataException
	 *             on data error such as validating, serializing, etc.
	 * @see #getConnection()
	 */
	@Override
	public void writeData(InputStream inputStream) throws IOException, DataException {
		
		URL url;
		try {
			url = getURLConnection();
		} catch ( Exception e ) {
			throw new AccessorException("write error: " + e.getMessage());
		}
		
		urlConnection = access(url.openConnection());
		urlConnection.setDoOutput(true);
		
		inputStream = serialize(validate(inputStream));
		OutputStream outputStream = urlConnection.getOutputStream();
		
		ByteStreams.copy(inputStream, outputStream);
		
		if ( ! sswapIndividual.isOfType(DataFormatType) ) {
			sswapIndividual.addType(DataFormatType);
		}
		
		close();
		
	}
	
	/**
	 * Return the first, valid Accessor URL, or the individual's URL if there
	 * are no accessors.
	 * 
	 * @return url URL for the individual or one of its accessors
	 * @throws DataException inability to establish any URL for this individual
	 */
	protected URL getURLConnection() throws DataException {
		
		URL url = null;
		
		Set<URI> accessors = getAccessors();
		if ( ! accessors.isEmpty() ) {
			for ( Iterator<URI> itr = accessors.iterator(); itr.hasNext() && url == null; ) {
				try {
					url = itr.next().toURL();
				} catch ( Exception e ) {
					;
				}
			}
			
		} else if ( ! sswapIndividual.isAnonymous() ) {
			try {
				url = sswapIndividual.getURI().toURL();
			} catch ( Exception e ) {
				throw new DataException("data is not a valid URL, nor has it a valid data:hasAccessor URL: " + sswapIndividual.getURI());
			}
		}
		
		if ( url == null ) {
			throw new DataException("data is not a valid URL, nor has it a valid data:hasAccessor URL");
		}
		
		return url;
		
	}
	
	/**
	 * Default implementation just passes urlConnection untouched. Custom
	 * implementations may extend this class and override this method. Note that
	 * accessing is a filter: accepting the urlConnection to access and
	 * returning the urlConnection for the next downstream step to read or
	 * write.
	 * 
	 * @param urlConnection
	 *            a URL connection opened on the data element individual (the
	 *            individual itself or one of its Accessors)
	 * @return URLConnection open URL connection
	 * @see HTTPBasicAuthImpl
	 */
	@Override
	public URLConnection access(URLConnection urlConnection) throws IOException, AccessorException {
		return urlConnection;
	}
	
	/**
	 * Get the underlying URL connection. This method is usually only needed to
	 * act on read or write error conditions.
	 * 
	 * @return the URL connection; null if not set
	 * 
	 */
	public URLConnection getConnection() {
		return urlConnection;
	}
	
	/**
	 * Closes the URL connection and sets it to null.
	 */
	public void close() {
		
		try {
			if ( urlConnection != null ) {
				urlConnection.getInputStream().close();
			}
		} catch ( Exception e) {
			;	// consume
		} finally {
			urlConnection = null;
		}
		
	}
	
	protected void finalize() throws Throwable {
		close();
	}

}
