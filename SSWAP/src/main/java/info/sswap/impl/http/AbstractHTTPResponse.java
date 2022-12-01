/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.http;

import info.sswap.api.http.HTTPResponse;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.impl.empire.Vocabulary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import com.clarkparsia.utils.web.Header;
import com.clarkparsia.utils.web.Response;

/**
 * An <code>AbstractHTTPResponse</code> consists of a SSWAPDocument as the
 * return pay load of an invocation, along with the network response object.
 * Methods are supplied to get the SSWAPDocument and the response, for example
 * to query the return code or header information.
 * <p>
 * If a document is successfully stored, then the underlying connection is
 * closed and the document may be read from <code>getDocument</code>. Otherwise
 * (on error), the connection is left open for reading and should be closed by
 * the caller.
 * <p>
 * The class is marked abstract to indicate that <code>getDocument</code> should
 * be overridden to return a specific type of document, such as:
 * <p>
 * <code>
 * &nbsp;&nbsp;RDG getRDG() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;return (RDG) getDocument();<br>
 * &nbsp;&nbsp;}
 * </code>
 * 
 * @see info.sswap.api.model.SSWAPDocument
 */
public abstract class AbstractHTTPResponse implements HTTPResponse {

	private static final String SSWAP_HTTP_EXCEPTION_HEADER = Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER;
	
	private Response response;
	private SSWAPDocument sswapDocument;
	private Exception exception;

	/**
	 * The constructor sets the SSWAPDocument and response object. Connection is
	 * automatically closed in the constructor if and only if there is a
	 * non-null SSWAPDocument.
	 * 
	 * @param sswapDocument
	 *            the resultant SSWAPDocument from the invocation (e.g., RDG);
	 *            may be null to indicate a failure to create a SSWAPDocument.
	 *            In that case, the response object may be interrogated and
	 *            should be closed when finished.
	 * @param response
	 *            the network Response object from the invocation; may be
	 *            null if it was impossible to establish an HTTP connection
	 */
	public AbstractHTTPResponse(SSWAPDocument sswapDocument, Response response) {
		this(sswapDocument, response, null /* exception */);
	}
	
	/**
	 * The constructor sets the SSWAPDocument and response object. Connection is
	 * automatically closed in the constructor if and only if there is a
	 * non-null SSWAPDocument.
	 * 
	 * @param sswapDocument
	 *            the resultant SSWAPDocument from the invocation (e.g., RDG);
	 *            may be null to indicate a failure to create a SSWAPDocument.
	 *            In that case, the response object may be interrogated and
	 *            should be closed when finished.
	 * @param response
	 *            the network Response object from the invocation; may be
	 *            null if it was impossible to establish an HTTP connection
	 * @param exception 
	 * 			  the exception that occurred during the invocation (may be null
	 *            for successful invocations).
	 */
	public AbstractHTTPResponse(SSWAPDocument sswapDocument, Response response, Exception exception) {
		
		this.sswapDocument = sswapDocument;
		this.response = response;
		this.exception = exception;
		
		// if sswapDocument is null, we assume an error (i.e., the model was
		// not built) and thus keep the HTTP connection in the response open
		// so that one may call the errorStream(). Otherwise (on success),
		// close the connection on instantiation.
		if ( (sswapDocument != null) && (response != null) ) {
			try {
				response.close();
			} catch ( Throwable t ) {
				;	// consume (may already be closed)
			}
		}
	}
	
	/**
	 * Concrete class should use this method and
	 * cast to return a specific type of SSWAPModel.
	 * @return the response document, or null on failure.
	 */
	protected SSWAPDocument getDocument() {
		return sswapDocument;
	}
	
	/**
	 * @inheritDoc
	 */
	public InputStream getErrorStream() {
		if (response != null) {
			return response.getErrorStream();
		}
		
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public String getMessage() {
		if (response != null) {
			return response.getMessage();
		}
		
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Collection<String> getHeaders() {
		if (response == null) {
			return Collections.EMPTY_LIST;
		}

		Collection<Header> headers = response.getHeaders();
		ArrayList<String> headerList = new ArrayList<String>();
		
		for ( Header header : headers ) {
			headerList.add(header.getName());
		}
		
		return headerList;
	}

	/**
	 * @inheritDoc
	 */
	public String getHeaderValue(String headerName) {
		if (response == null) {
			return null;
		}
		
		// Headers can be multi-valued, e.g.:
		// text/xml, text/plain; q=0.8, text/html; q=0.7, text/ *; q=0.3, * /*; q=0.1 
		
		Header header = response.getHeader(headerName);
		return header != null ? header.getRawHeaderValue() : null;
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection<String> getHeaderValues(String headerName) {
		if (response == null) {
			return Collections.EMPTY_LIST;
		}
		
		Header header = response.getHeader(headerName);
		return header != null ? header.getRawValues() : null;
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection<String> getSSWAPExceptionValues() {
		if (exception != null) {
			return Arrays.asList(exception.toString());
		}
		
		Collection<String> c = getHeaderValues(SSWAP_HTTP_EXCEPTION_HEADER);
		
		return c != null ? c : new HashSet<String>();
		
	}
	
	/**
	 * @inheritDoc
	 */
	public String getSSWAPExceptionHeader() {
		return SSWAP_HTTP_EXCEPTION_HEADER;
	}
	
	/**
	 * Not currently exposed in the RRGResponse interface.
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * @inheritDoc
	 */
	public int getResponseCode() {
		if (response == null) {
			return -1;
		}
		
		return response.getResponseCode();
	}

	/**
	 * @inheritDoc
	 */
	public InputStream getContent() {
		if (response != null) {		
			return response.getContent();
		}
		
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public boolean hasErrorCode() {
		if (response == null) {
			return true;
		}
		
		return response.hasErrorCode();
	}

	/**
	 * @inheritDoc
	 */
    public void close() throws IOException {
    	if (response != null) {
    		response.close();
    	}
    }

}
