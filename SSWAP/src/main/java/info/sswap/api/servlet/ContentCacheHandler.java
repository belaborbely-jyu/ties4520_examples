/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.impl.empire.Vocabulary;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler for HTTP requests for the content cache.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ContentCacheHandler {
	/**
	 * The name of the parameter in the query string that should contain the token
	 */
	public static final String TOKEN_PARAM = "token";
	
	/**
	 * A singleton instance of this class
	 */
	private static final ContentCacheHandler instance = new ContentCacheHandler();
	
	/**
	 * Accessor to the singleton instance
	 * 
	 * @return the singleton instance
	 */
	public static final ContentCacheHandler get() {
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
			
			ContentCache.Entry entry = ContentCache.get().get(token);
			
			if (entry == null) {
				// if we do not know anything about the token -- just return 404
				sendError(response, HttpServletResponse.SC_NOT_FOUND, null);
				return;
			}
			
			byte[] content = entry.getContent(); 
			
			if (content == null) {				
				sendError(response, HttpServletResponse.SC_NOT_FOUND, null);
				
				return;
			}
			
			// at this point RRG is not null, and should be returned to the client
			response.setContentType(entry.getContentType());
			response.setContentLength(content.length);
			
			ServletOutputStream httpResponseStream = null;

			try {
				httpResponseStream = response.getOutputStream();
				httpResponseStream.write(content);
			} finally {
				if ( httpResponseStream != null ) {
					httpResponseStream.close();
				}
			}					
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
