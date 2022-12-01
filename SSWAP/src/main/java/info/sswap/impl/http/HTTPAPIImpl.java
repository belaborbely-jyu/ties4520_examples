/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.clarkparsia.utils.web.Response;

import info.sswap.api.http.HTTPClient;
import info.sswap.api.http.HTTPProvider;

import info.sswap.api.model.PDG;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPProtocol;
import info.sswap.api.model.SSWAPResource;
import info.sswap.impl.empire.model.ModelUtils;

public class HTTPAPIImpl implements HTTPProvider, HTTPClient {
	
	private Response response = null;
	
	private static final String SSWAP_API_URI_SYSTEM_PROPERTY = "sswap.api.uri";
	
	private static final String SSWAP_API_URI_DEFAULT_VALUE = "http://sswap.info/api/";
		
	private static URI MAKE_PDG_URI = URI.create(System.getProperty(SSWAP_API_URI_SYSTEM_PROPERTY , SSWAP_API_URI_DEFAULT_VALUE) + "makePDG");
	
	private static URI MAKE_RDG_URI = URI.create(System.getProperty(SSWAP_API_URI_SYSTEM_PROPERTY, SSWAP_API_URI_DEFAULT_VALUE) + "makeRDG");;
	
	private static URI MAKE_RIG_URI = URI.create(System.getProperty(SSWAP_API_URI_SYSTEM_PROPERTY, SSWAP_API_URI_DEFAULT_VALUE) + "makeRIG");;

	private static URI MAKE_RRG_URI = URI.create(System.getProperty(SSWAP_API_URI_SYSTEM_PROPERTY, SSWAP_API_URI_DEFAULT_VALUE) + "makeRRG");;
	
	private static URI MAKE_RQG_URI = URI.create(System.getProperty(SSWAP_API_URI_SYSTEM_PROPERTY, SSWAP_API_URI_DEFAULT_VALUE) + "makeRQG");;


	/**
	 * @inheritDoc
	 */
	@Override
	public PDGResponse makePDG(InputStream jsonStream) throws IOException {
		
		PDG pdg = (PDG) makeAPI(MAKE_PDG_URI,PDG.class,jsonStream);
		
		return new PDGResponse(pdg,response);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public PDGResponse makePDG(String jsonString) throws IOException {
		return makePDG(new ByteArrayInputStream(jsonString.getBytes()));
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public RDGResponse makeRDG(InputStream jsonStream) throws IOException {
		
		RDG rdg = (RDG) makeAPI(MAKE_RDG_URI,RDG.class,jsonStream);
		
		return new RDGResponse(rdg,response);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public RDGResponse makeRDG(String jsonString) throws IOException {
		return makeRDG(new ByteArrayInputStream(jsonString.getBytes()));
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public RIGResponse makeRIG(InputStream jsonStream) throws IOException {
		
		RIG rig = (RIG) makeAPI(MAKE_RIG_URI,RIG.class,jsonStream);
		
		return new RIGResponse(rig,response);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public RIGResponse makeRIG(String jsonString) throws IOException {
		return makeRIG(new ByteArrayInputStream(jsonString.getBytes()));
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public RRGResponse makeRRG(InputStream jsonStream) throws IOException {
		
		RRG rrg = (RRG) makeAPI(MAKE_RRG_URI,RRG.class,jsonStream);
		
		return new RRGResponse(rrg,response);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public RRGResponse makeRRG(String jsonString) throws IOException {
		return makeRRG(new ByteArrayInputStream(jsonString.getBytes()));
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public RQGResponse makeRQG(InputStream jsonStream) throws IOException {
		
		RQG rqg = (RQG) makeAPI(MAKE_RQG_URI,RQG.class,jsonStream);
		
		return new RQGResponse(rqg,response);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public RQGResponse makeRQG(String jsonString) throws IOException {
		return makeRQG(new ByteArrayInputStream(jsonString.getBytes()));
	}

	
	private <T extends SSWAPDocument> T makeAPI(URI apiURI, Class<T> clazz, InputStream jsonStream) throws IOException {
		
		T sswapDocument = null;
		response = ModelUtils.invoke(apiURI,jsonStream,true);	// true = return response even on HTTP error
		
		if ( ! response.hasErrorCode() ) {
			try {
				sswapDocument = setDocumentURI(SSWAP.getResourceGraph(response.getContent(), clazz), clazz);
				
				
				try { response.close(); } catch ( Exception e ) { ; }
			} catch ( Exception e ) {
				throw new IOException(e.getMessage());
			}
		}
		
		return sswapDocument;	// may be null
		
	}
	
	private <T extends SSWAPDocument> T setDocumentURI(T originalDoc, Class<T> clazz) throws IOException {
		if (originalDoc instanceof SSWAPProtocol) {
			SSWAPResource resource = ((SSWAPProtocol) originalDoc).getResource();
			
			if ((resource != null) && (resource.getURI() != null)) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				originalDoc.serialize(bos);
				
				ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
				
				return SSWAP.getResourceGraph(bis, clazz, resource.getURI());
			}
		}
		
		return originalDoc;
	}
 
	/**
	 * Exposes access to both a ready-to-use <code>PDG</code> on success or a
	 * network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public class PDGResponse extends AbstractHTTPResponse implements info.sswap.api.http.HTTPProvider.PDGResponse {

		/**
		 * Constructor to set the <code>PDG</code> and network response data;
		 * not needed by most users.
		 * <p>
		 * The network connection is automatically closed on a successful
		 * <code>PDG</code> creation.
		 * 
		 * @param pdg
		 *            <code>PDG</code> to be returned to the user, or null
		 * @param response
		 *            network response object with response status information
		 */
		public PDGResponse(PDG pdg, Response response) {
			super(pdg, response);
		}
		
		/**
		 * @inheritDoc
		 */
		@Override
		public PDG getPDG() {
			return (PDG) getDocument();
		}

	}
	
	/**
	 * Exposes access to both a ready-to-use <code>RDG</code> on success or a
	 * network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public class RDGResponse extends AbstractHTTPResponse implements info.sswap.api.http.HTTPProvider.RDGResponse {

		/**
		 * Constructor to set the <code>RDG</code> and network response data;
		 * not needed by most users.
		 * <p>
		 * The network connection is automatically closed on a successful
		 * <code>RDG</code> creation.
		 * 
		 * @param rdg
		 *            <code>RDG</code> to be returned to the user, or null
		 * @param response
		 *            network response object with response status information
		 */
		public RDGResponse(RDG rdg, Response response) {
			super(rdg, response);
		}
		
		/**
		 * @inheritDoc
		 */
		@Override
		public RDG getRDG() {
			return (RDG) getDocument();
		}

	}
	
	/**
	 * Exposes access to both a ready-to-use <code>RIG</code> on success or a
	 * network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public class RIGResponse extends AbstractHTTPResponse implements info.sswap.api.http.HTTPClient.RIGResponse {

		/**
		 * Constructor to set the <code>RIG</code> and network response data;
		 * not needed by most users.
		 * <p>
		 * The network connection is automatically closed on a successful
		 * <code>RIG</code> creation.
		 * 
		 * @param rig
		 *            <code>RIG</code> to be returned to the user, or null
		 * @param response
		 *            network response object with response status information
		 */
		public RIGResponse(RIG rig, Response response) {
			super(rig, response);
		}
		
		/**
		 * @inheritDoc
		 */
		@Override
		public RIG getRIG() {
			return (RIG) getDocument();
		}
	}
	
	/**
	 * Exposes access to both a ready-to-use <code>RRG</code> on success or a
	 * network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public class RRGResponse extends AbstractHTTPResponse implements info.sswap.api.http.HTTPProvider.RRGResponse {

		/**
		 * Constructor to set the <code>RRG</code> and network response data;
		 * not needed by most users.
		 * <p>
		 * The network connection is automatically closed on a successful
		 * <code>RRG</code> creation.
		 * 
		 * @param rrg
		 *            <code>RRG</code> to be returned to the user, or null
		 * @param response
		 *            network response object with response status information
		 */
		public RRGResponse(RRG rrg, Response response) {
			super(rrg, response);
		}
		
		/**
		 * Constructor to set the <code>RRG</code> and an exception;
		 * not needed by most users.
		 * <p>
		 * 
		 * @param rrg
		 *            <code>RRG</code> to be returned to the user, or null
		 * @param response 
		 *            network response object with response status information
		 * @param exception 
		 *            exception that prevented RRG creation from succeeding
		 */
		public RRGResponse(RRG rrg, Response response, Exception exception) {
			super(rrg, response, exception);
		}

	
		/**
		 * @inheritDoc
		 */
		@Override
		public RRG getRRG() {
			return (RRG) getDocument();
		}

	}
	
	/**
	 * Exposes access to both a ready-to-use <code>RQG</code> on success or a
	 * network response object on failure.
	 * 
	 * @author Damian Gessler <dgessler@iplantcollaborative.org>
	 * 
	 */
	public class RQGResponse extends AbstractHTTPResponse implements info.sswap.api.http.HTTPClient.RQGResponse {

		/**
		 * Constructor to set the <code>RQG</code> and network response data;
		 * not needed by most users.
		 * <p>
		 * The network connection is automatically closed on a successful
		 * <code>RQG</code> creation.
		 * 
		 * @param rqg
		 *            <code>RQG</code> to be returned to the user, or null
		 * @param response
		 *            network response object with response status information
		 */
		public RQGResponse(RQG rqg, Response response) {
			super(rqg, response);
		}
		
		/**
		 * @inheritDoc
		 */
		@Override
		public RQG getRQG() {
			return (RQG) getDocument();
		}

	}
}
