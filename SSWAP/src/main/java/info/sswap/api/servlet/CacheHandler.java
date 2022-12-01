/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.api.model.RRG;
import info.sswap.impl.empire.Vocabulary;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler for HTTP requests for the cache (e.g., polling for RRGs). The cache-related requests are forwarded to this
 * class by AbstractSSWAPServlet
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class CacheHandler {
	/**
	 * The name of the parameter in the query string that should contain the token
	 */
	public static final String TOKEN_PARAM = "token";
	
	/**
	 * The name of the HTTP field in the response to the polling request that should contain the suggested
	 * polling interval (in milliseconds)
	 */
	public static final String POLLING_INTERVAL_HEADER = "X-SSWAP-Suggested-Polling-Interval";
	
	public static final String EXECUTION_STATUS_HEADER = "X-SSWAP-Execution-Status";
	
	/**
	 * A singleton instance of this class
	 */
	private static final CacheHandler instance = new CacheHandler();
	
	/**
	 * Accessor to the singleton instance
	 * 
	 * @return the singleton instance
	 */
	public static final CacheHandler get() {
		return instance;
	}
	
	/**
	 * Handles an HTTP GET request for the cache
	 * 
	 * @param request the request
	 * @param response the response
	 * @throws ServletException
	 */
	public final void handleGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		String token = request.getParameter(TOKEN_PARAM);

		try {
			if (token == null) {
				// HTTP 400 for all requests without ?token=
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Required parameter missing (token) in the query string");
				return;
			}
			
			RRGCache.Entry entry = RRGCache.get().get(token);
			
			if (entry == null) {
				// if we do not know anything about the token -- just return 404
				sendError(response, HttpServletResponse.SC_NOT_FOUND, null);
				return;
			}
			
			RRG rrg = entry.getRRG(); 
			
			if (entry.getStatus() != null) {
				response.addHeader(EXECUTION_STATUS_HEADER, entry.getStatus());
			}			
			
			if (rrg == null) {
				if (entry.getErrorMessage() != null) {
					// HTTP 502 to indicate that the service (to which this cache is a gateway) failed
					response.setHeader(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER, entry.getErrorMessage());
									
					sendError(response, HttpServletResponse.SC_BAD_GATEWAY, entry.getErrorMessage());
				}
				else {
					// otherwise, we are going to return 204 with an optional HTTP header with suggested polling interval
					if (entry.getSuggestedPollingInterval() > 0) {
						response.addHeader(POLLING_INTERVAL_HEADER, String.valueOf(entry.getSuggestedPollingInterval()));
					}
					
					response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				}
				return;
			}
			
			// at this point RRG is not null, and should be returned to the client
			response.setContentType("application/rdf+xml");
			
			ServletOutputStream httpResponseStream = null;

			try {
				httpResponseStream = response.getOutputStream();
				rrg.serialize(httpResponseStream);
			} finally {
				if ( httpResponseStream != null ) {
					httpResponseStream.close();
				}
			}
			
			rrg.serialize(response.getOutputStream());			
		}
		catch (IOException e) {
			throw new ServletException("I/O error while sending response", e);
		}
	}
	
	private void sendError(HttpServletResponse response, int status, String errMsg) throws IOException {
			response.setContentType("application/json");
			
		if ( errMsg == null || errMsg.isEmpty() ) {
			errMsg = "<Error message not recoverable>";
		}
		
		response.setHeader(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER, errMsg);

			response.setStatus(status);

			PrintWriter printWriter = response.getWriter();

			// a JSON object with the SSWAP HTTP Exception header
			// as the key and the errMsg as the string value
			printWriter.format("{\n  \"%s\" : \"%s\"\n}\n",Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER, errMsg);
			printWriter.close();
	}

}
