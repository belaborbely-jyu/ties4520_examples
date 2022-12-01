/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.api.http.HTTPProvider;
import info.sswap.api.model.*;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.empire.model.ModelUtils;
import info.sswap.ontologies.sswapmeet.SSWAPMeet.Exec;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.openrdf.model.vocabulary.RDF;

import com.clarkparsia.utils.web.*;
import com.google.common.io.ByteStreams;

/**
 * Handles HTTP GETs and POSTs to a SSWAP service point. This class is a bridge
 * between handling a HTTP request and the SSWAP Java API that allows on-demand,
 * transaction-time reasoning to satisfy the request.
 * <p>
 * Note: A simple way to implement a service without the constraints and
 * subtleties of servlet programming is to use a servlet launcher to launch a
 * regular Java class. For most developers this is the recommended way; see
 * {@link SimpleSSWAPServlet} and {@link MapsTo}
 * <p>
 * To use, extend this abstract class and override the
 * <code>handleRequest</code> method. When an Resource Invocation Graph (
 * <code>RIG</code>) is sent to the servlet, <code>handleRequest</code> will
 * allow action on the <code>RIG</code> to create a Resource Response Graph (
 * <code>RRG</code>) to be returned back to the client. The <code>RIG</code>
 * supports a <code>translate</code> method to allow semantic mapping of the
 * <code>RIG</code> into the vocabulary and concepts of the service's Resource
 * Description Graph (<code>RDG</code>).
 * <p>
 * Upon return from <code>handleRequest</code>, this class generates an
 * <code>RRG</code> which is serialized back to the client.
 * <p>
 * The servlet responds to HTTP GETs and POSTs in the following manner:
 * <ul>
 * <li>GET with no query string returns the <code>RDG</code>;
 * 
 * <li>GET with a query string initiates auto-invocation whereby the servlet
 * creates an <code>RIG</code> from the GET query string and the service's
 * <code>RDG</code>. The service is then self-invoked. During GET query string
 * parsing, query string terms are semantically matched to terms on the
 * <code>SSWAPResource</code> or <code>SSWAPSubject</code>s of the
 * <code>RDG</code>. An <code>RIG</code> is generated and the service is invoked
 * as if it was POSTed the <code>RIG</code>.
 * 
 * <li>POST should contain a RIG as the body of the POST. Upon receipt, the
 * service is invoked. Exercise care if POSTing from a HTML {@code <form>}
 * element so as to not prepend the POST body with a parameter name. If
 * necessary, use Javascript to POST the contents directly or POST to a handler
 * servlet that extracts the <code>RIG</code> content and POSTs only it to the
 * service. URLs within the <code>RIG</code> may be URL encoded (if needed), but
 * the <code>RIG</code> itself should be plain RDF/XML.
 * </ul>
 * <p>
 * GET query string parameters (<i>e.g.</i>,
 * <i>http://.../MyService?property=value&prefix:property=value2</i>) are
 * converted into service parameters according to the following rules:
 * <p>
 * <ul>
 * <li><i>property=value</i>; <i>property</i> is prefixed with the default
 * namespace of the RDG and mapped to a fully qualified URI (ontology term) and
 * assigned the value;
 * <li><i>prefix:property=value</i>; where <i>prefix</i> is a prefix defined in
 * the <code>RDG</code>. <i>prefix:property</i> is resolved to a fully qualified
 * URI (ontology term) and assigned the value;
 * <li><i>prefix:=uri</i>; where <i>prefix</i> is not defined in the RDG. Allows
 * the setting of a new prefix. This may occur anywhere in the query string and
 * applies to all occurrences of <i>prefix</i> used in the query string;
 * <li><i>http://.../property=value</i> retained as-is, assigning the value to
 * the fully qualified URI.
 * <li>anything else is interpreted as an argument for the SSWAP Exec package.
 * </ul>
 * <p>
 * By default, <i>property=value</i> assignments are matched against statements
 * on the <code>RDG</code>'s <code>SSWAPSubject</code>s. To instead force a
 * match against the <code>SSWAPResource</code> properties, prepend a tilde (~)
 * immediately before the prefix or property: <i>e.g.</i>,
 * <i>~property=value</i> or <i>~prefix:property=value</i>. The tilde (~) is a
 * query string flag only: it is not part of the property name.
 * <p>
 * To use the servlet, create a typical mapping in your servlet container's
 * web.xml file, such as:
 * <p>
 * 
 * <pre>
 * {@code
 * <servlet>
 *   <servlet-name>MyServlet</servlet-name>
 * 
 *     <servlet-class>org.mySite.sswap.MyServlet</servlet-class>
 * 
 *     <!-- if not defined, will be derived from <url-pattern>
 *     <init-param>
 *       <param-name>RDGPath</param-name>
 *       <param-value>/pathTo/MyRDG</param-value>
 *     </init-param>
 *     -->
 * </servlet>
 * 
 * <servlet-mapping>
 *   <servlet-name>MyServlet</servlet-name>
 *   <url-pattern>/MyService/*</url-pattern> <!-- always end in /* -->
 * </servlet-mapping>
 * }
 * </pre>
 * 
 * Replace and customize the values for {@code <servlet-name>},
 * {@code <servlet-class>}, {@code <param-value>}, and {@code <url-pattern>} per
 * usual web.xml practices. All other values should be as above. In the above
 * example, <code>MyServlet extends AbstractSSWAPServlet</code> and defines
 * <code>handleRequest</code>. The use and definition of <code>RDGPath</code> is
 * optional but recommended. If it is not defined, then the RDG must reside at
 * the request URI; if it is defined, then its value must be the path to a valid
 * <code>RDG</code> on the local web server.
 * <p>
 * Error handling: <br>
 * If an error is due to a client misconfigured <code>RIG</code>, then an error
 * message is returned to the client and set in the HTTP header. An
 * <code>RRG</code> is not returned to the client. But if the <code>RIG</code>
 * passes validation--both syntactically as OWL RDF/XML, and semantically as
 * being complaint with the RDG--but yet an error occurs on the server-side,
 * then the <code>RIG</code> is returned to the client without semantic
 * modification (still, no <code>RRG</code>).
 * <p>
 * 
 * @see #handleRequest
 * @see SimpleSSWAPServlet
 * @see MapsTo
 * @see info.sswap.api.model.RDG
 * @see info.sswap.api.model.RIG
 * @see info.sswap.api.model.RRG
 * @see info.sswap.api.model.SSWAPResource
 * @see info.sswap.api.model.SSWAPSubject
 * 
 * @author Damian Gessler
 */
public abstract class AbstractSSWAPServlet extends HttpServlet {
	
	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Interface to Logging API
	 */
	private static final Logger LOGGER = LogManager.getLogger(AbstractSSWAPServlet.class);
	
	/**
	 * The suffix to the request string for RRG retrieval; that is, if a request
	 * ends with this suffix (attached to the regular service's URL), it indicates
	 * that the request is for retrieving a cached RRG, and not invoking the service.
	 */
	public static final String RRG_RETRIEVAL_SUFFIX = "/rrg";
	
	/**
	 * Real path to the RDG on the file system as specified in web.xml (may be a directory)
	 */
	private String rdgPath = null;
	
	/**
	 * Absolute, fully resolved path and file name of the RDG on the file system
	 */
	private String rdgFile = null;

	/**
	 * Flag string appended to terms in the GET query string to identify them as
	 * pertaining to the SSWAPResouce
	 */
	private final static String resourceFlag = "~";
		
	/**
	 * URI to be called to pass on service invocation to a remote service. Only
	 * used when this servlet is extended, as by SSWAPProxyServlet, which sets
	 * the value to non-null.
	 */
	protected URI remoteServiceURI = null;

	/**
	 * Suggested polling interval read from web.xml (if defined) or a default value.
	 * This field contains an interval (in milliseconds) that will be returned as
	 * an advice to callers that invoke this service in an asynchronous way.
	 * 
	 * (During asynchronous invocation, this servlet immediately returns an
	 * asynchronous RRG, even though the service has not computed its result;
	 * later the caller is supposed to poll (inquiry) at this interval to verify
	 * whether the actual result is ready for retrieval.) 
	 */
	private int suggestedPollingInterval;
	
	/**
	 * Flag to check if init has completed.
	 */
	private boolean initCompleted = false;

	/**
	 * Override this method (or {@link GenericServlet#init()} for custom servlet
	 * initialization code. If overriding this method, overriding method MUST call
	 * {@code super.init(servletConfig) } as the first line of code. This method
	 * is called once at the end of all internal initialization and before any
	 * requests are serviced.
	 * <p>
	 * The method is passed the Servlet Configuration and may throw a
	 * ServletException. Thrown errors are not caught and will terminate servlet
	 * initialization.
	 * <p>
	 * Uses for this method are to set the servlet timeout (see
	 * {@link #setTimeout}), effect changes to URI caching (see
	 * {@link SSWAP#getCache}), and so forth.
	 * 
	 * @param servletConfig
	 *            the ServletConfig object of the servlet
	 * @throws ServletException
	 *             on a servlet configuration error
	 * 
	 * @see info.sswap.api.model.SSWAP
	 * @see javax.servlet.ServletConfig
	 */
	public synchronized void init(ServletConfig servletConfig) throws ServletException {

		super.init(servletConfig);
		
		// optional: set value in web.xml via an entry such as:
		/*
		 * <servlet>
		 * 	  <servlet-name>MyServlet</servlet-name>
		 * 	  <servlet-class>org.mySite.MyServlet</servlet-class>
		 *    <init-param>
		 *      <param-name>RDGPath</param-name>
		 *      <param-value>/MyServices</param-value>
		 *   </init-param>
		 * </servlet>
		 */

		if ( (rdgPath = servletConfig.getInitParameter("RDGPath")) != null ) {
			rdgPath = getServletContext().getRealPath(rdgPath);
		}
				
		// retrieve the optional suggested polling interval from web.xml 
		// if the value is not set in web.xml, we take the default value from RRGCache
		//
		// the value can be set in web.xml as
		/*
		 *  <init-param>
		 *    <param-name>SuggestedPollingInterval</param-name>
		 *    <param-value>5000</param-value>
		 *  </init-param>
		 */
		if (servletConfig.getInitParameter("SuggestedPollingInterval") != null) {
			try {
				suggestedPollingInterval = Integer.parseInt(servletConfig.getInitParameter("SuggestedPollingInterval"));
			}
			catch (NumberFormatException e) {
				suggestedPollingInterval = RRGCache.DEFAULT_SUGGESTED_POLLING_INTERVAL;
			}
		}
		else {
			suggestedPollingInterval = RRGCache.DEFAULT_SUGGESTED_POLLING_INTERVAL;
		}
		
		// retrieve the optional properties file from web.xml 
		// if the value is not set in web.xml, we take the default value from Config
		//
		// the value can be set in web.xml as
		/*
		 *  <init-param>
		 *    <param-name>ConfigPath</param-name>
		 *    <param-value>pathTo/myConfig.properties</param-value>
		 *  </init-param>
		 */
		// For just setting the async timeout, see setTimeout() to do this programmatically
		
		String configPath = servletConfig.getInitParameter("ConfigPath");
		
		if (configPath != null) {
			try {
				FileInputStream fis = new FileInputStream(getServletContext().getRealPath(configPath));				
				Config.get().load(fis);
				fis.close();
			}
			catch (IOException e) {
				throw new ServletException("Unable to read the configuration from the specified file: " + configPath);
			}
		}
		
		// start with an empty cache to assure the that any current
		// run-time ontology changes are freshly dereferenced
		// (unless there exists NoCacheFlush parameter in web.xml that is set to true)
		// the value in web.xml can be set as follows:
		/*
		 * <init-param>
		 *   <param-name>NoCacheFlush</param-name>
		 *   <param-value>true</param-value>
		 * </init-param>
		 */
		if ((servletConfig.getInitParameter("NoCacheFlush") == null) || !Boolean.valueOf(servletConfig.getInitParameter("NoCacheFlush"))) {
			LOGGER.info("Cleaning term cache");
			SSWAP.getCache().clear();	
		}		
		
		initCompleted = true;
	}
	
	
	/**
	 * The servlet timeout value, in milliseconds.
	 * 
	 * @return the timeout value, or -1 on any error that precludes retrieving a
	 *         value
	 */
	public int getTimeout() {
		
		int timeout;
		try {
			timeout = Integer.valueOf(Config.get().getProperty(Config.RIG_INVOCATION_TIMEOUT_KEY));
		} catch ( Exception e ) {
			timeout = -1;
		}
		
		return timeout > 0 ? timeout : -1;

	}
	
	/**
	 * Set servlet timeout value, in milliseconds. Increase this value if the
	 * web service needs additional time to complete.
	 * <p>
	 * Timeout values less than 1000 (1 sec) are silently ignored.
	 */
	public void setTimeout(int timeout_ms) {
		
		if ( timeout_ms >= 1000 ) {
			try {
				Config.get().setProperty(Config.RIG_INVOCATION_TIMEOUT_KEY,Integer.toString(timeout_ms));
			} catch ( Exception e ) {
				; // silently ignore
			}
		}
	}
	

	/**
	 * Initialization per request
	 * 
	 * @throws ServletException if servlet init() method is not called
	 */
	private void initializeRequest(HttpServletRequest request) throws ServletException {

		if ( ! initCompleted ) {
			throw new ServletException("servlet init(servletConfig) did not complete; if overriding, call super.init(servletConfig) first");
		}
		
		String requestURI;
		try {
			requestURI = URLDecoder.decode(request.getRequestURI(),"UTF-8");  // does not include query string
		} catch ( UnsupportedEncodingException e ) {
			return;
		}
		
		if ( rdgPath == null ) {
			
			// strip the context path from the requestURI
			String pathURI = requestURI;
			String contextPath = request.getContextPath();
			
			if ( requestURI.startsWith(contextPath) ) {
				
				pathURI = requestURI.substring(contextPath.length());
				
				if ( ! pathURI.startsWith("/") ) {
					pathURI = "/" + pathURI;
				}
			}
	
			rdgFile = getServletContext().getRealPath(pathURI);
			
		} else {
			
			File file = new File(rdgPath);
			
			if ( file.isDirectory() ) {
				
				String name = (new File(requestURI)).getName();
				if ( ! rdgPath.endsWith(File.separator) ) {
					name = File.separator + name;
				}
				rdgFile = rdgPath + name;
				
			} else if ( ! file.isFile() ) {
				rdgFile = null;
			} else { // no change if rdgPath is already a File; read capability is checked at access time
				rdgFile = rdgPath;
			}
		}
		
		// Simple catch to intercept naive circular references when the
		// remoteServiceURI is already handled by this servlet
		if ( remoteServiceURI != null ) {

			String remoteServiceStr = remoteServiceURI.toString();
			String requestURLStr = request.getRequestURL().toString();

			if ( remoteServiceStr.equals(requestURLStr) ) {
				remoteServiceURI = null;
			}
		}
		
		// may return with rdgFile == null
	}

	/**
	 * An HTTP GET equates to returning an <code>RDG</code> or creating a
	 * just-in-time <code>RIG</code> from the query string and invoking this
	 * service.
	 * <p>
	 * This method is marked <code>protected</code> solely for package access
	 * purposes. It should not be called directly and cannot be overridden. The
	 * servlet handler will call this method automatically on an HTTP GET. To
	 * handle the request, override the method <code>handleRequest()</code>.
	 * <p>
	 * An HTTP GET with no (extra) path info and no query string equates to a
	 * request for the RDG (Resource Description Graph).
	 * <p>
	 * An HTTP GET with a query string equates to an invocation, with measures
	 * taken to semantically resolve the query string parameters in terms of
	 * this RDG's semantics. A RIG is automatically generated from the query
	 * string and this service is invoked.
	 * <p>
	 * 
	 * @param request
	 *            HTTP Servlet request
	 * @param response
	 *            HTTP Servlet response
	 * @throws ServletException
	 *             as thrown by the servlet container
	 * @see #handleRequest(RIG rig)
	 * @see RDG
	 * @see RIG
	 * @see RRG
	 */
	@Override
	@SuppressWarnings("unchecked")	// for (Map<String,String[]>) request.getParameterMap()
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {

		int responseCode = -1;	// default, undefined HTTP response code
		
		initializeRequest(request);
		
		if (request.getRequestURL().toString().endsWith(RRG_RETRIEVAL_SUFFIX)) {
			CacheHandler.get().handleGet(request, response);
			return;
		}

		try {

			// strips query string and implements the function of GETing the content
			RDG rdg = getRDG(request.getRequestURL().toString()); 
			String queryStr = request.getQueryString();

			// check to see if we should simply return the RDG
			if ( queryStr == null ) {
				serializeResponse(request, response, rdg); // Return the RDG
			} else { // parameters in the query string; parse and invoke the service

				// The resultant RRG from invoking the service
				RRG rrg;

				// The default case is for this servlet to handle actions on the RDG.
				// If remoteServiceURI is set, then this servlet acts as a proxy to
				// the remoteServiceURI, and will invoke it

				if ( remoteServiceURI == null ) { // default case

					// Get the RIG for this RDG

					RIG rig = rdg.getRIG();
					
					// make a mutable copy of the immutable parameter map
					Map<String,String[]> parameterMap = makeParameterMap(request);
					
					// Substitute (add/replace) properties of the servlet RDG
					// with mapped (translated) query string parameters
					resolveParameters(rig, parameterMap);

					// POST to this servlet and get an RRG
					HTTPProvider.RRGResponse rrgResponse = rig.invoke();

					// copy over any SSWAP Exception headers
					String exceptionHeader = rrgResponse.getSSWAPExceptionHeader();
					for ( String exceptionMsg : rrgResponse.getSSWAPExceptionValues() ) {
						response.setHeader(exceptionHeader,exceptionMsg);
					}
					
					rrg = rrgResponse.getRRG(); // will throw on validation and other errors
					if ( rrg == null ) {
						
						responseCode = rrgResponse.getResponseCode();
						String errMsg = "HTTP error (" + responseCode + "):";
						
						Exception exception = rrgResponse.getException();
						if ( exception != null ) {
							errMsg = exception.getMessage();
						} else {
							errMsg = rrgResponse.getMessage();
						}
						
						if ( errMsg == null || errMsg.trim().isEmpty() ) {
							errMsg = "cannot secure response from " + rig.getURI().toString();
						}
						
						if ( responseCode > HttpServletResponse.SC_OK ) {
							errMsg = "HTTP error (" + responseCode + "): " + errMsg;
						}
						throw new IOException(errMsg);
					}

				} else { // an RDG and a remoteServiceURI

					// invoke the remote service without any query string
					// translation; will throw on error
					URI uri = new URI(remoteServiceURI.toString() + "?" + request.getQueryString());
					rrg = invokeRemoteService(uri.normalize());
				}

				// return the response
				serializeResponse(request, response, rrg);

			}

		} catch ( Throwable t ) { // any and all, including RuntimeException

			try {
				sendError(request, response, t.getMessage(), responseCode);
			} catch ( IOException ioe ) {
				throw new ServletException(t.getMessage());
			}
		}

	}

	/**
	 * An HTTP POST equates to a request service invocation.
	 * <p>
	 * This method is marked <code>protected</code> solely for package access
	 * purposes. It should not be called directly and cannot be overridden. The
	 * servlet handler will call this method automatically on an HTTP POST. To
	 * handle the request, override the method <code>handleRequest()</code>.
	 * <p>
	 * 
	 * @param request
	 *            HTTP Servlet request
	 * @param response
	 *            HTTP Servlet response
	 * @throws ServletException
	 *             as thrown by the servlet container
	 * @see #handleRequest(RIG rig)
	 */
	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

		int responseCode = -1;	// default, undefined HTTP response code
		
		InputStream inputStream = null;

		initializeRequest(request);

		try {

			// implements the function of GETing the content
			RDG rdg = getRDG(request.getRequestURL().toString());
			RIG asyncRIG = null;
			RIG rig = null;
			RRG rrg = null;
			SSWAPProtocol sswapProtocol = null;

			try {

				inputStream = request.getInputStream();

				if ( remoteServiceURI == null ) { // default case

					/*
					 * Basic behavior is that if an error is due to a client
					 * misconfigured RIG, then an error (no RRG) is returned to
					 * the client. But if the RIG is OK, but an error occurs on
					 * the server-side, then the RIG is returned to the client
					 * without semantic modification (still, no RRG).
					 * 
					 * There is a special case when handleRequest() may often
					 * return a valid RIG for conversion to a RRG--and ultimate
					 * return to the client--but at some times, network
					 * conditions affecting dereferencing may result in a
					 * validation error on the RRG (due to incomplete term
					 * descriptions). This case is handled below, with the
					 * client's original input returned.
					 */

					// Save a copy of the input stream in a buffer
					byte[] buf = ByteStreams.toByteArray(inputStream);
					ByteArrayInputStream byteArraryInputStream = new ByteArrayInputStream(buf);

					// check whether this is an asynchronous RIG
					try {
						asyncRIG = SSWAP.getResourceGraph(byteArraryInputStream, RIG.class);
						
						if (!isAsyncRIG(asyncRIG)) {
							asyncRIG =  null;
						}
					} catch ( Throwable t ) {
						
						LOGGER.info("Cannot parse RIG: " + t);
						
						int length = buf.length < 123 ? buf.length : 123;
						String str = new String(buf,0,length,"UTF-8");
						
						if ( length == 123 ) {
							str += " ...";
						} else if ( str.isEmpty() ) {
							str = "<nothing>";
						}
						
						if (t instanceof InconsistentOntologyException) {
							throw new DataAccessException("Invalid RIG: " + t.getMessage());
						}
						
						throw new DataAccessException("Invalid RIG: expected RDF/XML but got: " + str);
						
					} finally {
						// reset the stream so that it can be read again (either to be processed as a regular RIG 
						// or to make the copy of the asynchronous RIG)
						byteArraryInputStream.reset();
					}

					if ( asyncRIG == null ) { // regular invocation
												
						// Get the RIG for this RDG
						try {
							rig = rdg.getRIG(byteArraryInputStream);
						} catch ( Exception e ) {
							LOGGER.info("Cannot process RIG; validation exception: " + e);
							throw e;  // bail and return an exception message
						}
						
						// perform the customized service
						try {
							
							// user should override this method; could throw for any reason
							handleRequest(rig);
							
							// convert to RRG and validate
							sswapProtocol = rrg = rig.getRRG();

							// combine and merge owl:sameAs individuals
							//sswapProtocol.reduce();

						} catch ( ClientException ce ) {
								responseCode = ce.getResponseCode();
								throw ce;			// will result in no content (no RIG) returned
						} catch ( Throwable t ) { 	// could not get a valid RRG; return the RIG back to the caller
							
							LOGGER.error("handleRequest on RIG failed with the following exception",  t);

							response.setHeader(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER, t.getMessage());
							byteArraryInputStream.reset();
							
							// will throw on parsing error, but does not validate RIG against RDG
							sswapProtocol = rig = SSWAP.getResourceGraph(byteArraryInputStream, RIG.class);

						}
						
					} else { // asynchronous invocation
						
						// generate token which can be used later by the client to retrieve the result
						String rrgToken = getRRGToken();
						
						// create entry in the RRG cache with the token -- the entry contains only the suggested polling interval
						// (i.e., no RRG yet since the result has not yet been computed)
						RRGCache.get().setSuggestedPollingInterval(rrgToken, suggestedPollingInterval);
						
						// modify the asynchronous RIG and token to the resource
						SSWAPResource resource = asyncRIG.getResource();
						resource.setProperty(asyncRIG.getPredicate(URI.create(Vocabulary.TOKEN.toString())), rrgToken);
						
						// make the copy of the original asyncRIG before passing it to the separate thread
						// (SSWAP objects are not thread-safe)
						RIG asyncRIGCopy = null;
						
						try {
							asyncRIGCopy = SSWAP.getResourceGraph(byteArraryInputStream, RIG.class);														
						} finally {
							byteArraryInputStream.reset();
						}
						
						// prepare asynchronous invocation (with the copy)
						AsyncRIGInvocation asyncRIGInvocation = new AsyncRIGInvocation(asyncRIGCopy, rdg, rrgToken);
						
						// start the new thread and start executing the service there
						new Thread(asyncRIGInvocation).start();
						
						// assignment to return asyncRIG below
						sswapProtocol = asyncRIG;
					}

				} else {  // invoke the remote service; ; will throw on error
					sswapProtocol = rrg = invokeRemoteService(remoteServiceURI, inputStream);
				}

				// return RIG, RRG, or asyncRRG as response
				serializeResponse(request, response, sswapProtocol);


			} catch ( Throwable t ) { // any and all, including RuntimeException
				sendError(request, response, t.getMessage(), responseCode);
			}

		} catch ( Throwable t ) { // any and all, including RuntimeException
			throw new ServletException(t.getMessage());
		} finally {

			if ( inputStream != null ) {
				try {
					inputStream.close();
				} catch ( Exception e ) {
					; // consume
				}
			}
		}

	}

	/**
	 * Verifies whether the submitted RIG is an asynchronous RIG.
	 * The determination is done based on analyzing sswap:Subject's types -- if
	 * subject is typed with a marker class async:RRG, then it is an asynchronous RIG.
	 * 
	 * @param asyncRIGCandidate a RIG to be checked
	 * @return true if the RIG is an asynchronous RIG, false otherwise
	 */
	private boolean isAsyncRIG(RIG asyncRIGCandidate) {
		SSWAPResource resource = asyncRIGCandidate.getResource();
		
		if (resource != null) {
			SSWAPGraph graph = resource.getGraph();
			
			if (graph != null) {
				SSWAPSubject subject = graph.getSubject();
				
				if ( (subject != null) && subject.getDeclaredTypes().contains(asyncRIGCandidate.getType(URI.create(Vocabulary.ASYNC_RRG.toString()))) ) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	/**
	 * Return an error message.
	 * 
	 * Generate error message response as a JSON object, if possible. If the
	 * request Accept header is "text/html", then this is overridden and
	 * HttpServletResponse.sendError() is called with its consequent formatted
	 * HTML response.
	 */
	private void sendError(HttpServletRequest request, HttpServletResponse response, String errMsg, int responseCode) throws IOException {

		boolean returnJSON = isMIMETypeAcceptable(request,"application/json, text/plain"); // true on */*
		boolean returnHTML = returnJSON ? false : isMIMETypeAcceptable(request,"text/html");

		// default, return JSON
		if ( returnJSON ) {
			// Browsers may accept anything (e.g., */*), but if actually sent 'application/json'
			// may give the user a Save file dialog box. This is usually not wanted for error messages,
			// so we use 'text/plain' even for JSON content.
			response.setContentType("text/plain");
		} else if ( ! returnHTML ) {
			response.setContentType("text/plain");
			returnJSON = true;
		}

		if ( errMsg == null || errMsg.isEmpty() ) {
			errMsg = "<Error message not recoverable>";
		}

		try {
			JSONObject result = new JSONObject();
			result.put(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER, errMsg);

			response.setHeader(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER, result.toString(2));  // toString() pretty print indentation
		}
		catch (JSONException e) {
			throw new IOException("Unable to generate JSON for exception: ", e);
		}

		LOGGER.error("Error message being returned to caller: " + errMsg);

		if ( responseCode <= 0 ) {
			responseCode = HttpServletResponse.SC_BAD_REQUEST;
		}
		
		if ( returnJSON ) {

			response.setStatus(responseCode);

			PrintWriter printWriter = response.getWriter();
			printWriter.println(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER + ": " + errMsg);
			printWriter.close();

		} else { // will produce HTML
			response.setContentType("text/html");
			response.sendError(responseCode, errMsg);
		}
	}

	/**
	 * Override this method to convert an incoming Resource Invocation Graph
	 * <code>RIG</code> into an outgoing Resource Response Graph
	 * <code>RRG</code>. Edits to the <code>RIG</code> will be the foundation of
	 * the <code>RRG</code> returned from this service call. Sample code:
	 * 
	 * <pre>
	 * {@code
	 * public void handleRequest(RIG rig) {
	 * 
	 * // if we need to check service parameters we could do it here
	 * //SSWAPResource translatedResource = rig.getTranslatedResource();
	 * 
	 * // loop over every subject, across all matching graphs
	 * for ( SSWAPSubject translatedSubject : rig.getTranslatedSubjects() ) {
	 * 
	 * 	// "translation" maps types and properties in the RIG
	 * 	// into the vocabulary we understand in the RDG.
	 * 
	 * 	// for all objects for which the current subject is already stating a mapping
	 * 	for ( SSWAPObject sswapObject : translatedSubject.getObjects() ) {
	 * 		// do something:
	 * 		// edit object based on the type(s) and property values of the subject
	 * 	}
	 * 
	 * 	// if and as necessary, add additional object mappings to subject
	 * 	SSWAPObject sswapObject;
	 * 	try {	// if and as appropriate
	 * 		sswapObject = rig.createObject(new URI("http://mySite.org/someData"));
	 * 	} catch ( Exception e ) {
	 * 		sswapObject = rig.createObject();	// a "blank node"
	 * 	}
	 * 
	 * 	// do something:
	 * 	// edit object based on the type(s) and property values of the subject
	 * 
	 * 	// add it to the subject
	 * 	translatedSubject.addObject(sswapObject);
	 * }
	 * 
	 * // done
	 * 
	 * // for testing, you may call getRRG()
	 * // if it throws a validation exception, the RRG will not be returned to the caller;
	 * // on error, the caller will get the original RIG returned unchanged
	 * boolean debugging = false;
	 * if ( debugging ) {
	 * 	  try {
	 * 		rig.getRRG();	// expensive
	 * 	  } catch ( Exception e ) {
	 * 		System.err.println("Failed to create a valid RRG:");
	 * 		rig.serialize(System.err);
	 * 	  }
	 *  }
	 * 
	 * }
	 * }
	 * </pre>
	 * 
	 * Providers should do their best to satisfy the request of the RIG, but
	 * they are not required to be exhaustive. For example, SSWAPObjects should
	 * informatively satisfy the contract of the mapping from a SSWAPSubject,
	 * but they do not need themselves to extend a deep graph of relations. The
	 * decision on how much data to return is left to the provider. Regardless,
	 * what data is returned must satisfy the logical contract of the RDG.
	 * <p>
	 * If a request is larger than the provider wishes to satisfy (<i>e.g.</i>,
	 * hundreds or thousands of SSWAPSubjects each requiring database calls),
	 * the provider may satisfy none, a few, or all at its choosing. If the
	 * provider wants to satisfy none--<i>i.e.</i>, it handles requests on an
	 * all-or-none basis--and it wants the current state returned as an error to
	 * the client, it may return an HTTP 413 Request Entity Too Large response
	 * code by throwing the runtime exception RequestEntityTooLargeException. In
	 * this case no content (neither RIG nor RRG) is be returned to the client.
	 * <p>
	 * If the provider is unable to access data, it should fail silently and allow
	 * the RIG to be returned.  If the problem is due to a client error, for
	 * example, a missing URL, then the provider may throw a ClientException
	 * to return an error to the client.
	 * 
	 * @param rig
	 *            <code>RIG</code> invoking the service. This <code>RIG</code>
	 *            should be edited and will become the basis for the
	 *            <code>RRG</code> returned by the service. Best practice is to
	 *            leave most of the <code>RIG</code> untouched, modifying only
	 *            the <code>SSWAPObject</code> subgraphs.
	 * 
	 * @see info.sswap.api.model.RIG
	 * @see info.sswap.api.model.RRG
	 * @see RequestEntityTooLargeException
	 * @see ClientException
	 * 
	 */
	protected abstract void handleRequest(RIG rig);


	/**
	 * Dereference the servlet's RDG and map any Exceptions to a general
	 * IOException.  Will catch and throw on rdgURIStr null or empty errors.
	 */
	private RDG getRDG(String rdgURIStr) throws IOException {

		RDG rdg = null;

		InputStream inputStream = null;

		try { // dereference, but do not get closure
			
			if ( rdgURIStr == null || rdgURIStr.isEmpty() ) {
				throw new Exception("Cannot read RDG (Resource Description Graph): URL not set");
			}


			try {
				
				URI uri = new URI(rdgURIStr);
				inputStream = new FileInputStream(rdgFile);
				if ( (rdg = SSWAP.getResourceGraph(inputStream, RDG.class, uri)) == null ) { // URISyntaxException
					throw new Exception();
				}
				
			} catch ( Exception e ) {
				throw new Exception("Cannot read RDG (Resource Description Graph) for URL: " + rdgURIStr);
			}

		} catch ( Exception e ) {
			throw new IOException(e.getMessage());
		} finally {
			if ( inputStream != null ) {
				try {
					inputStream.close();
				} catch ( Exception e ) {
					; // consume
				}
			}
		}

		return rdg;
	}

	/**
	 * Invoke a HTTP GET; Wrapper to invokeRemoteService(URI remoteService,null)
	 */
	private RRG invokeRemoteService(URI remoteService) throws IOException, ValidationException {
		return invokeRemoteService(remoteService, null);
	}

	/**
	 * Invoke a HTTP GET or POST on 'remoteService'. 'bodyStream' is the content
	 * of the POST, or may equal null to invoke a GET.
	 * 
	 * Return the response as a validated RRG, or throw on any error
	 */
	private RRG invokeRemoteService(URI remoteService, InputStream bodyStream) throws IOException, ValidationException {

		Response response = null;
		RRG rrg;

		try {

			// invoke the service
			try {
				response = ModelUtils.invoke(remoteService, bodyStream); // throws IOException
			} catch ( Exception e ) {
				throw new IOException("Error invoking remote service at: " + remoteService.toString());
			}

			// parse the RRG (catch RuntimeExceptions)
			try {
				rrg = SSWAP.getResourceGraph(response.getContent(), RRG.class);
			} catch ( Throwable t ) {
				throw new IOException("Error parsing RRG (Resource Response Graph) from invoking remote service at: " + remoteService.toString());
			}

			rrg.validate(); // will throw ValidationException on error

		} finally {

			if ( response != null )
				try {
					response.close();
				} catch ( Exception e ) {
					; // consume
				}
		}

		return rrg;
	}

	
	/**
	 * Parse the HTTP servlet request into a "mutable" parameter:value map.
	 * URLDecodes both parameters and their values before storing in the map.
	 * 
	 * @param request HTTP servlet request with possible parameters
	 * @return a mutable map of parameter:value mappings
	 */
	private Map<String,String[]> makeParameterMap(HttpServletRequest request) {
		
		// make a mutable copy of the immutable parameter map
		Map<String,String[]> parameterMap = new HashMap<String,String[]>();

		// URL decode every query string name and value
		// e.g. ?myPrefix%3Aterm=hello+world -> myPrefix:term with value "hello world"
		Enumeration<String> names = request.getParameterNames();
		try {
			while( names.hasMoreElements() ) {

				String key = URLDecoder.decode(names.nextElement(),"UTF-8");
				String[] values = request.getParameterValues(key);

				for ( int i = 0; i < values.length; i++ ) {
					values[i] = URLDecoder.decode(values[i],"UTF-8");
				}

				parameterMap.put(key, values);
			}
		} catch ( UnsupportedEncodingException uee ) {
			try {
				parameterMap.putAll((Map<String,String[]>) request.getParameterMap());
			} catch ( Exception e ) {
				; // consume
			}
		}
				
		return parameterMap;
	}
	
	
	/**
	 * Resolves query string parameters against name spaces to get absolute
	 * URIs. Then resolves these URIs against SSWAPProperties (using reasoning),
	 * to merges (add or replace) SSWAPProperties with the appropriate semantic
	 * mappings based on reasoning over the RIG.
	 */
	private void resolveParameters(RIG rig, Map<String,String[]> queryStringParameterMap) throws IllegalArgumentException {

		// set prefix namespaces, if any, from query string
		setNamespaces(rig,queryStringParameterMap);
		
		// parse the query string into a map of resolved (absolute URIs) and their values
		ParameterList parameterList = resolveQueryString(rig, queryStringParameterMap);
		
		// extract just those parameters for the SSWAPResource (if any)
		// modify parameterList accordingly
		ParameterList resourceList = partitionParameters(parameterList);
		
		// get its SSWAPResource
		SSWAPResource rdgResource = rig.getResource();

		// resolve for the RIG's SSWAPResource
		resolveProperties(rig,rdgResource,resourceList);
		
		// for every sswapGraph... for every sswapSubject
		for (SSWAPGraph sswapGraph : rdgResource.getGraphs()) {
			for (SSWAPSubject sswapSubject : sswapGraph.getSubjects()) {
				resolveProperties(rig, sswapSubject, parameterList);
			}
		}
		
	}

	/**
	 * Parse the GET query string parameter map for prefixes (namespace
	 * assignments) and set the namespaces in the rig's NsPrefixMap. Prefixes
	 * that are IANA schemes are passed through--not set as prefixes. Thus, for
	 * example, "urn:=http://someSite.org/" cannot be (re)defined as a namespace
	 * prefix.
	 * 
	 * @param rig
	 *            document (model) to set new namespaces
	 * @param queryStringParameterMap
	 *            source parameter:value mapping
	 */
	private void setNamespaces(RIG rig, Map<String,String[]> queryStringParameterMap) {
		
		// save map entries to remove until end
		ArrayList<String> keysToRemove = new ArrayList<String>();
		
		// get prefix map
		Map<String, String> prefixMap = rig.getNsPrefixMap();

		// for every putative prefix assignment (e.g., prefix:=http://...)
		for ( String key : queryStringParameterMap.keySet() ) {
			
			// prefixes are identified as keys ending with ':'
			if ( key.endsWith(":") ) {
				
				String prefix = key.substring(0,key.length()-1);
				
				// security issue: won't allow query string to redefine prefixes or reserved namespaces
				if ( prefixMap.containsKey(prefix) || ModelUtils.isSchemeKnown(prefix) ) {
					continue;
				}
				
				URI namespace = null;
				
				try {
					namespace = URI.create(queryStringParameterMap.get(key)[0]);
				} catch ( Exception e ) { // many ways to fail
					continue;
				}
				
				keysToRemove.add(key);
				rig.setNsPrefix(prefix,namespace);
				
			}
		}
		
		// remove prefix definitions from query string parameter map
		for ( String key : keysToRemove ) {
			queryStringParameterMap.remove(key);
		}
		
	}
	
	
	/**
	 * Map query string parameters into fully qualified URLs or URNs.
	 * 
	 * Examples:
	 * 
	 * Query string Parameter mapping<br>
	 * ------------ -----------------<br>
	 * <ul>
	 * <li>?key=value => <default namespace>/key, value(s)
	 * <li>?matchedPrefix:key=value => <qualified (resolved) path>/key, value(s)
	 * <li>?http://someSite.org/someTerm=value => http://someSite.org/someTerm, value(s)
	 * <li>?{@code}<other> => <dropped>{/@code}
	 * </ul>
	 * 
	 * @param rig The RIG with a prefix map
	 * 
	 * @param queryStringParameterMap parameter map of parameters and vales, as from request.getParameterMap()
	 * 
	 * @return a mapping of resolved URLs or URNs and a string array of values for each entry
	 */
	private ParameterList resolveQueryString(RIG rig, Map<String, String[]> queryStringParameterMap) {

		// Note: our query string syntax (e.g., &prefix:term=value) does not map to
		// javax.xml.namespace.QName syntax: qname = "{" + Namespace URI + "}" + local part
		// so we do not use the QName API here

		String	args = "",	// exec:args
				resourcePrefix;
		
		// place to store QName resolution of query string parameters and values
		ParameterList parameterList = new ParameterList();

		// namespace prefixes
		Map<String, String> prefixMap = rig.getNsPrefixMap();

		// for every request parameter ...
		for ( String key : queryStringParameterMap.keySet() ) {

			String qualifiedName = null;

			// save, but strip resourceFlag
			if ( key.startsWith(resourceFlag) ) {
				key = key.substring(resourceFlag.length(), key.length());
				resourcePrefix = resourceFlag;
			} else {
				resourcePrefix = "";
			}
			
			// 'key' is "term" or "prefix:term" or "http://..."
			// A QName such as "prefix:term" is a valid URI, but not a valid URL,
			// so we enforce URL checking first, and then use the URI construct for parsing

			try {
				new URL(key); // if it's a valid URL, it's not a QName, so use as-is
				qualifiedName = key;

			} catch ( MalformedURLException me ) { // not a URL; is it a URN (QName)?

				try {

					URI urn = new URI(key);

					// URN = [scheme:]scheme-specific-part[#fragment]
					// QName = [prefix:]term
					String prefix = urn.getScheme();
					
					if ( prefix == null ) {
						
						String values[] = queryStringParameterMap.get(resourcePrefix + key);
						if ( values.length == 1 && values[0].isEmpty() ) {
							args += " " + key;	// interpret as an un-parameterized argument
							continue;
						}
						
						prefix = "";	// default namespace, if defined
					}

					String term = urn.getSchemeSpecificPart(); // never null
					String frag = urn.getFragment();
					if ( frag != null ) {
						term += "#" + frag; // rare
					}

					if ( (qualifiedName = prefixMap.get(prefix)) != null ) {
						qualifiedName += term;
					} else { // cannot resolve; build as exec:args argument
						args += " " + key;
						continue;
					}

				} catch ( URISyntaxException ue ) {
					args += " " + key;
					continue; // not a URI (neither URL nor URN [QName])
				}
			}

			if ( qualifiedName != null ) {

				// save qualified terms (as URI strings) and their values
				for ( String value : queryStringParameterMap.get(resourcePrefix + key) ) {
					parameterList.add(resourcePrefix + qualifiedName, value);
				}

			}

		}
		
		if ( ! args.isEmpty() ) {
			
			// pass through an args property only if the sswapResource
			// already has an args property or is type ExecCmd
			
			String argsPredicateStr = resourceFlag + Exec.args.toString();

			for ( Parameter parameter : parameterList ) {
				if ( parameter.getName().equalsIgnoreCase(argsPredicateStr) ) {
					
					args = parameter.getValue() + " " + args;
					parameterList.remove(parameter);
					break;
				}
			}
			
			try {
				
				SSWAPResource sswapResource = rig.getResource();
				SSWAPPredicate argsPredicate = rig.getPredicate(Exec.args);
				SSWAPProperty argsProperty = sswapResource.getProperty(argsPredicate);
				
				if ( argsProperty != null || sswapResource.isOfType(rig.getType(Exec.ExecCmd)) ) {
					parameterList.add(resourceFlag + Exec.args.toString(),args.trim());
				}
				
			} catch ( Exception e ) {
				;	// consume
			}
		}

		return parameterList;
	}

	/**
	 * Partition the parameter list by removing (and returning a list of) those
	 * parameters that will apply to the only the SSWAPResource.
	 * 
	 * @param parameterList
	 *            original list of parameters for both SSWAPSubject and
	 *            SSWAPResource
	 * @return param parameterList of those only for SSWAPResource. These are
	 *         removed from the original parameter list.
	 */
	private ParameterList partitionParameters(ParameterList parameterList) {
		
		final String keyFlag = resourceFlag;
		
		ParameterList resourceList = new ParameterList();
		ParameterList removeList = new ParameterList();
		
		for ( Parameter param : parameterList ) {
			
			String name = param.getName();
			
			if ( name.startsWith(keyFlag) ) {
				name = name.substring(keyFlag.length(),name.length());
				resourceList.add(new Parameter(name,param.getValue()));
				removeList.add(param);
			}
		}
		
		parameterList.removeAll(removeList);
		
		return resourceList;
		
	}
	
	
	/**
	 * Removes all properties from sswapNode and adds back only those properties
	 * (parameters) from resolvedQNameMap that are ObjectProperties or
	 * DatatypeProperties
	 */
	private void resolveProperties(RIG rig, SSWAPNode sswapNode, ParameterList resolvedQNameMap) throws IllegalArgumentException {

		// sswapNode will be either a SSWAPSubject or the SSWAPResource
		
		// clear all properties first (so as not to disrupt the later addition
		// of more than one instance of a property from the query string)
		for ( Parameter parameter : resolvedQNameMap ) {

			String uriStr = parameter.getName();
			SSWAPPredicate queryStringPredicate;

			try {
				queryStringPredicate = rig.getPredicate(new URI(uriStr));
				if ( queryStringPredicate.isReserved() ) {
					continue;
				}
			} catch ( Exception e ) { // URIException
				continue; // just skip it
			}
			
			// for functional datatype property subsumption all instances must first be cleared
			sswapNode.clearProperty(queryStringPredicate);
		}
		
		// for every resolved QName query string parameter
		// (Parameters with multiple values will appear multiple times in resolvedQNameMap)

		for ( Parameter parameter : resolvedQNameMap ) {

			String uriStr = parameter.getName();
			String value = parameter.getValue();

			SSWAPPredicate queryStringPredicate;

			try {
				
				queryStringPredicate = rig.getPredicate(new URI(uriStr));
				
				// special handling for setting rdf:type from a GET query string
				if ( queryStringPredicate.isReserved() ) {
					
					// queryStringPredicate.getURI().equals(RDF.TYPE) failing despite equal string representations
					String queryStringURIStr = queryStringPredicate.getURI().toString();
					String rdfTypeStr = RDF.TYPE.stringValue();
					
					if ( queryStringURIStr.equalsIgnoreCase(rdfTypeStr) ) {
						try {							
							SSWAPType sswapType = rig.getType(new URI(value));
							sswapNode.addType(sswapType);
						} catch ( Exception e ) {
							; // consume and fall-through
						}
					}
					
					continue;
				}
				
			} catch ( Exception e ) { // URIException
				continue; // just skip it
			}				
				
			// use reasoning service to see if this is an owl:ObjectProperty
			if ( queryStringPredicate.isObjectPredicate() ) {

				try {
					// we'll assign here, even though this could make the model
					// inconsistent (e.g., if the property has a range to which
					// this individual cannot belong). That check will be done
					// when the RIG is processed on invocation.
					SSWAPIndividual sswapIndividual = rig.createIndividual(new URI(value));
					sswapNode.addProperty(queryStringPredicate, sswapIndividual);
					
				} catch (Exception e) {
					continue; // ignore parameter
				}

			} else if ( queryStringPredicate.isDatatypePredicate() ) {

				SSWAPLiteral sswapLiteral = null;
				String range = queryStringPredicate.getDatatypePredicateRange();

				// try to set the literal's datatype; e.g., XSD
				if ( range != null ) {
					try {
						sswapLiteral = rig.createTypedLiteral(value, new URI(range));
					} catch ( Exception e ) { // IllegalArgumentException, URISyntaxException
						throw new IllegalArgumentException(e.getMessage());
					}
				} else {
					sswapLiteral = rig.createLiteral(value);
				}

				sswapNode.addProperty(queryStringPredicate, sswapLiteral);

			} else { // e.g., AnnotationProperty
				// if handling annotation properties, need to distinguish between
				// resource and literal objects (e.g., re rdfs:seeAlso and rdfs:isDefinedBy)
				continue;	// consume; ignore
			}
					
		}
		
	}

	/**
	 * Generates an RRG token for an asynchronous invocation. 
	 * 
	 * @return an RRG token
	 */
	private String getRRGToken() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Publishes an RRG (so that it can be retrieved by the caller in the asynchronous invocation protocol).
	 * 
	 * @param token token under which the RRG should be published
	 * @param rrg RRG to be published
	 */
	void publishRRG(String token, RRG rrg) {
		RRGCache.get().store(token, rrg);
	}
	
	/**
	 * Extracts sswap:outputURI from the sswap:Resource in this SSWAPModel.
	 * 
	 * @param model the model from which the sswap:outputURI should be extracted
	 * @return a string containing the outputURI or null
	 */
	private String extractOutputURI(SSWAPModel model) {
		// we can only extract outputURI from protocol messages (RDG/RIG/RRG/RQG etc)
		if ((model != null) && (model instanceof SSWAPProtocol)) {
			SSWAPProtocol protocolModel = (SSWAPProtocol) model;
			SSWAPResource resource = protocolModel.getResource();
			
			// check whether there is a valid sswap:Resource
			if (resource != null) {				
				URI outputURI = resource.getOutputURI();
				
				// convert sswap:outputURI to String and return, only if one is available
				return (outputURI == null)?  null : outputURI.toString();
			}
		}
		
		return null;	
	}

	/**
	 * Serialize the model back to the HTTP response.
	 */
	private void serializeResponse(HttpServletRequest request, HttpServletResponse response, SSWAPModel model) throws IOException {

		response.setContentType("application/rdf+xml");
		ServletOutputStream httpResponseStream = null;

		try {
			String outputURI = extractOutputURI(model);
			
			if (outputURI != null) {
				response.addHeader(Vocabulary.SSWAP_OUTPUT_URI_HEADER, outputURI);
			}
			
			httpResponseStream = response.getOutputStream();
			model.serialize(httpResponseStream);

		} catch ( Exception e ) {  // any and all, including RuntimeExceptions
			throw new IOException("Unable to serialize (write-out) model for: " + model.getURI().toString());
		} finally {
			if ( httpResponseStream != null ) {
				httpResponseStream.close();
			}
		}
	}

	/*
	 * methods for parsing Accept headers and MIME types
	 */

	/**
	 * Parses a list of MIME types, as they occur in the Accept header; for
	 * example:
	 * 
	 * text/xml, text/plain; q=0.8, text/html; q=0.7, text/ *; q=0.3, * /*;
	 * q=0.1
	 * 
	 * @param mimeTypeList
	 *            string containing a mime type list
	 * @return an array of mime types (without q values)
	 */
	private static String[] parseMIMETypeList(String mimeTypeList) {

		// split the list on the comma
		String[] result = mimeTypeList.split(",");

		for ( int i = 0; i < result.length; i++ ) {
			
			// get rid of the surrounding spaces
			result[i] = result[i].trim();

			// get rid of anything after a ';' (usually a q value)
			if ( result[i].contains(";") ) {
				result[i] = result[i].split(";")[0];
			}
		}

		return result;
	}

	/**
	 * Returns the major type in a MIME type (everything up to the first '/'; if
	 * there is one)
	 * 
	 * @param mimeType
	 *            the MIME type
	 * @return the major type in a MIME type
	 */
	private static String getMajorMIMEType(String mimeType) {
		return mimeType.split("/")[0];
	}

	/**
	 * Returns the minor type in a MIME type (everything after the first '/'; if
	 * there is one)
	 * 
	 * @param mimeType
	 *            the MIME type
	 * @return the minor type in a MIME type (or empty string if there is none)
	 */
	private static String getMinorMIMEType(String mimeType) {
		String[] types = mimeType.split("/");

		if ( types.length > 1 ) {
			return types[1];
		}

		return "";
	}

	/**
	 * Checks whether a MIME type matches a MIME type pattern (i.e., a string
	 * that may contain '*" in the place of major type, minor type or both
	 * 
	 * @param pattern
	 *            the pattern
	 * @param mimeType
	 *            the MIME type
	 * @return true, if the pattern matches
	 */
	private static boolean isMIMETypeMatch(String pattern, String mimeType) {
		String patternMajorType = getMajorMIMEType(pattern);
		String majorType = getMajorMIMEType(mimeType);

		if ( ! patternMajorType.equals("*") && !patternMajorType.equals(majorType) ) {
			return false;
		}

		String patternMinorType = getMinorMIMEType(pattern);
		String minorType = getMinorMIMEType(mimeType);

		if ( ! patternMinorType.equals("*") && !patternMinorType.equals(minorType) ) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether the given MIME type is acceptable according to the values
	 * for the accept header
	 * 
	 * @param acceptHeaderValue
	 *            the value of the accept header
	 * @param mimeType
	 *            MIME type to be checked
	 * @return true if the MIME type is a valid value given the value of the
	 *         accept header
	 */
	private static boolean isMIMETypeAcceptable(String acceptHeaderValue, String mimeType) {

		for ( String acceptPattern : parseMIMETypeList(acceptHeaderValue) ) {
			for ( String matchPattern : parseMIMETypeList(mimeType) ) {
				if ( isMIMETypeMatch(acceptPattern, matchPattern) ) {
					return true;
				}
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	// for Enumeration<String> acceptHeaders = request.getHeaders("Accept");
	private static boolean isMIMETypeAcceptable(HttpServletRequest request, String mimeType) {

		Enumeration<String> acceptHeaders = request.getHeaders("Accept");

		if ( acceptHeaders != null ) {
			while ( acceptHeaders.hasMoreElements() ) {

				String header = acceptHeaders.nextElement().toLowerCase();
				if ( isMIMETypeAcceptable(header, mimeType) ) {
					return true;
				}
			}
		}

		return false;
	}
}
