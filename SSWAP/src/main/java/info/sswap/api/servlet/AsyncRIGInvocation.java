/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import info.sswap.api.http.HTTPProvider.RRGResponse;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPDatatype.XSD;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.Vocabulary;

/**
 * Provides an asynchronous invocation of a service with a RIG. The run() method of this class
 * is executed in a separate thread, and it wraps the usual, synchronous execution of a method. Later,
 * the result of the service's execution is stored in a cache, so that the caller may retrieve it.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
class AsyncRIGInvocation implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(AsyncRIGInvocation.class);
	
	/**
	 * The original asynchronous RIG submitted in this invocation
	 */
	private RIG asyncRIG;
	
	/**
	 * The RIG of the service being invoked.
	 */
	private RDG rdg;
	
	/**
	 * The token that will allow the caller to retrieve the resulting RRG.
	 */
	private String rrgToken;
	
	/**
	 * Creates a new asynchronous invocation object that will wrap in a separate thread the regular, synchronous execution
	 * of the service.
	 * 
	 * @param asyncRIG the asynchronous RIG submitted to invoke the service
	 * @param rdg the RDG for the service being invoked
	 * @param rrgToken the token that will allow the caller to retrieve the resulting RRG
	 */
	public AsyncRIGInvocation(RIG asyncRIG, RDG rdg, String rrgToken) {
		this.asyncRIG = asyncRIG;
		this.rdg = rdg;
		this.rrgToken = rrgToken;
	}

	/**
	 * Main method of the asynchronous thread. It retrieves the input RRG and converts it into regular (non-asynchronous) RIG,
	 * invokes the service synchronously, and publishes the resulting RRG.
	 */
    public void run() {
    	RRG upstreamRRG = null;
    	
    	try {
    		// retrieve RRG of the upstream service
    		 upstreamRRG = getUpstreamRRG(asyncRIG);
    		 
    		 if (LOGGER.isDebugEnabled()) {
    			 LOGGER.debug("Upstream RRG is");
    			 logDebug(upstreamRRG);
    		 }
    	}
    	catch (Throwable e) {
    		handleError("STEPHEN Unable to retrieve upstream RRG/data", e);
    		return;
    	}
    	
    	RIG rig = null;
    	
    	try {    		
    		// create the RIG from RRG
    		rig = createRIG(upstreamRRG);
    		
    		if (LOGGER.isDebugEnabled()) {
    			LOGGER.debug("RIG created from the upstream RRG");
    			logDebug(rig);
    		}
    	}
    	catch (Throwable e) {
    		handleError("Unable to convert the upstream data into a RIG", e);
    		return;
    	}
    	
    	try {    	
    		// perform regular (synchronous invocation of the service);    		
    		RRGResponse rrgResponse = rig.invoke();
    		
    		if (!rrgResponse.getSSWAPExceptionValues().isEmpty()) {
    			throw new IOException(rrgResponse.getSSWAPExceptionValues().iterator().next());
    		}
    		
    		// get RRG from the service's response
    		RRG resultRRG = rrgResponse.getRRG();
    		
    		// if there is a token inside (should be) -- remove it
    		removeToken(resultRRG);
    		
    		if (resultRRG == null) {
    			throw new DataAccessException("Invalid or no RRG received from service");
    		}
    		
    		// publish the RRG
    		RRGCache.get().store(rrgToken, resultRRG);
    	} 
    	catch (Throwable e) {
    		handleError(e);
    	}
    }
     
    private void handleError(Throwable e) {
    	LOGGER.error("Asynchronous invocation of a RIG failed", e);
    	RRGCache.get().setError(rrgToken, e.getMessage());
    }
    
    private void handleError(String description, Throwable e) {
    	LOGGER.error(description, e);
    	RRGCache.get().setError(rrgToken, description + ": " + e.getMessage());
    }
   
    /**
     * Creates a RIG from an upstream RRG.
     * 
     * @param upstreamRRG the upstream RRG
     * @return the created RIG
     */
    private RIG createRIG(RRG upstreamRRG) {
    	// first use standard SSWAP Java API methods to convert and RRG into a RIG given the service's RDG
    	RIG result = upstreamRRG.createRIG(rdg);
    	
    	// it is possible that the service requires parameters on sswap:Resource
    	// and these should have been provided in the asynchronous RIG.
    	// Therefore, we are going to copy all properties from sswap:Resource in asynchronous
    	
    	SSWAPResource asyncResource = asyncRIG.getResource();
    	SSWAPResource resultResource = result.getResource();
    	
    	Set<SSWAPPredicate> asyncPredicates = new HashSet<SSWAPPredicate>();
    	
    	for (SSWAPProperty resourceProp : asyncResource.getProperties()) {
    		asyncPredicates.add(resourceProp.getPredicate());
    	}
    	
    	// now clear all/any of the default values in the resulting (non-async) RIG's resource
    	for (SSWAPPredicate nonAsyncPredicate : asyncPredicates) {
    		resultResource.clearProperty(nonAsyncPredicate);
    	}
    	
    	// copy the values from the async RIG to the non-async RIG
    	for (SSWAPProperty resourceProp : asyncResource.getProperties()) {    		
    		SSWAPElement value = resourceProp.getValue();
    		
    		// deep copy element
    		SSWAPElement copiedValue = ExtensionAPI.copyElement(result, value);
    		
    		if (copiedValue.isIndividual()) {
    			resultResource.addProperty(result.getPredicate(resourceProp.getURI()), copiedValue.asIndividual());
    		}
    		else if (copiedValue.isLiteral()) {
    			resultResource.addProperty(result.getPredicate(resourceProp.getURI()), copiedValue.asLiteral());
    		}
    	}
    	
    	resultResource.setProperty(result.getPredicate(URI.create(Vocabulary.TOKEN.toString())), rrgToken, XSD.xstring);
    	
    	return result;
    }
    
    private void removeToken(RRG rrg) {
    	SSWAPResource resource = rrg.getResource();
    	
    	if (resource != null) {
    		SSWAPProperty tokenProperty = resource.getProperty(rrg.getPredicate(URI.create(Vocabulary.TOKEN.toString())));
    		
    		if (tokenProperty != null) {
    			resource.removeProperty(tokenProperty);
    		}
    	}
    }
    
    /**
     * Retrieves the upstream RRG given the information in the asynchronous RIG
     * 
     * @param asyncRIG the asynchronous RIG
     * @return the RRG
     * @throws Exception if there is an issue while retrieving the RIG.
     */
    private RRG getUpstreamRRG(RIG asyncRIG) throws Exception {
    	// Use URI of the subject in the async RIG to get the URI of the upstream
    	LOGGER.error("STEPHEN 1");
    	SSWAPSubject asyncSubject = asyncRIG.getResource().getGraph().getSubject();
    	LOGGER.error("STEPHEN 2");
    	RRG upstreamRRG = null;
    	LOGGER.error("STEPHEN 3");
    	URL upstreamRRGURL = asyncSubject.getURI().toURL();
    	LOGGER.error("STEPHEN 4");
    	
    	// open connection
    	InputStream is = upstreamRRGURL.openStream();
    	LOGGER.error("STEPHEN 5");

    	// create RRG
    	upstreamRRG = SSWAP.getResourceGraph(is, RRG.class);
    	LOGGER.error("STEPHEN 6");

    	is.close();
    	LOGGER.error("STEPHEN 7");
    	
    	return upstreamRRG;
    }
    
    private static void logDebug(SSWAPModel model) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		model.serialize(bos);
		LOGGER.debug(new String(bos.toByteArray()));
	}
}
