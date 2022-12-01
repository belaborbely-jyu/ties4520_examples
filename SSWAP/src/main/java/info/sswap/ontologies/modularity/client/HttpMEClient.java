/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.modularity.client;

import info.sswap.api.model.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author Pavel Klinov
 *
 */
public class HttpMEClient {

	/*
	 * used mostly for testing
	 */
	public static String getServicePath() {
		return Config.get().getProperty(Config.MODULE_EXTRACTION_URI_KEY);
	}
	
	public static String getMEURI(ModuleType modType, boolean derefUnknownURIs) {
		return Config.get().getProperty(Config.MODULE_EXTRACTION_URI_KEY) + "/me_"
				+ (derefUnknownURIs ? "on_demand_" : "")
				+ modType.toString();
	}
	
	public static String getTermResolutionURI() {
		return Config.get().getProperty(Config.MODULE_EXTRACTION_URI_KEY) + "/resolve";
	}	
	
	public Model extract(Collection<URI> terms, ModuleType moduleType, boolean expandInitialSignature) throws ModuleExtractionException {
		return extract(terms, moduleType, expandInitialSignature, false);
	}
	
	public Model extract(Collection<URI> terms, ModuleType moduleType, boolean expandInitialSignature, boolean derefUnknownURIs) throws ModuleExtractionException {
		
		if (terms == null || terms.isEmpty()) return ModelFactory.createDefaultModel();
		
		try {
			HttpClient httpclient = new HttpClient();
			PostMethod method = new PostMethod(getMEURI(moduleType, derefUnknownURIs));
			
			// Set parameters
			NameValuePair[] qparams = new NameValuePair[terms.size()];
			int i = 0;
			
			for (URI term : terms) {
				String paramName = null;
				//TODO support rdfs:isDefinedBy
				paramName = utf8Encode(term.toString());
				
				qparams[i++] = new NameValuePair(paramName, "");
			}
			
			method.setRequestBody(qparams);
			//Execute it
			httpclient.executeMethod(method);
			
			return readResponse(method.getResponseBodyAsStream());
			
		} catch (Throwable e) {
			throw new ModuleExtractionException(e);
		} 	
	}

	public static String utf8Encode(String theString) throws UnsupportedEncodingException {
		return URLEncoder.encode(theString, "UTF-8");
	}	
	
	private Model readResponse(InputStream stream) throws IllegalStateException, IOException {
		Model model = ModelFactory.createDefaultModel();
		
		model.read(stream, null);

		return model;
	}
	

	public Model resolveTerm(URI term, URI definedBy) throws ModuleExtractionException {
		try {
			HttpClient httpclient = new HttpClient();
			// Set the parameter
			String paramName = utf8Encode(term.toString());
				
			NameValuePair[] qparams = new NameValuePair[] {new NameValuePair(paramName, "")};
			
			PostMethod post = new PostMethod(getTermResolutionURI());
			
			post.setRequestBody(qparams);
			//Execute it
			httpclient.executeMethod(post);
			// Parse RDF/XML
			return readResponse(post.getResponseBodyAsStream());
		} catch (Exception e) {
			throw new ModuleExtractionException(e);
		}		
	}	
}
