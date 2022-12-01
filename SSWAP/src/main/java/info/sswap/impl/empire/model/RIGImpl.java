/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.Expressivity;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.http.HTTPAPIImpl;
import info.sswap.impl.http.HTTPAPIImpl.RRGResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.clarkparsia.utils.web.Response;


/**
 * Implementation of RIG.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public abstract class RIGImpl extends ProtocolImpl implements RIG {
	private RDG rdg;

    private Map<SSWAPNode,SSWAPNode> translatedNodes = new HashMap<SSWAPNode,SSWAPNode>();
    
    private Collection<SSWAPGraph> matchingGraphs;
    
    private boolean clientSideTranslation = true;
	
    public Collection<SSWAPGraph> getMatchingGraphs() {
    	if (matchingGraphs == null) {
    		return Collections.EMPTY_LIST;
    	}
    	
    	return matchingGraphs;
    }
    
    protected void setMatchingGraphs(Collection<SSWAPGraph> matchingGraphs) {
    	this.matchingGraphs = matchingGraphs;
    }
    
	/**
	 * @inheritDoc
	 */
    public RRG getRRG() throws DataAccessException, ValidationException {
    	RRGImpl result =  ImplFactory.get().createEmptySSWAPDataObject(getURI(), RRGImpl.class);
    	
		// read from this RIG
		result.dereference(this);		
		
		result.setOwlDlRequired(isOwlDlRequired());
		
		result.validate();
		
		if (rdg != null) {			
			result.validateAgainstRDG(rdg);
		}
		
		return result;    
	}

    /**
     * @inheritDoc
     */
    public RRG getRRG(InputStream is) throws DataAccessException {
    	RRGImpl result =  ImplFactory.get().createEmptySSWAPDataObject(getURI(), RRGImpl.class);
		
		result.dereference(is);		
		result.setMutable(false);
		
		if (!result.checkProfile(Expressivity.DL)) {
			result.setOwlDlRequired(false);
		}
		
		return result;    
	}
    
    RDG getRDG() {
    	return rdg;
    }
    
    void setRDG(RDG rdg) {
    	this.rdg = rdg;
    }
    
    void setClientSideTranslation(boolean clientSideTranslation) {
    	this.clientSideTranslation = clientSideTranslation;
    }

    @Override
    protected boolean supportsTranslation() {
    	return true;
    }
    
    @Override
    protected Map<SSWAPNode,SSWAPNode> getTranslationMap() {
    	return translatedNodes;
    }
    
	/**
	 * @inheritDoc
	 */
	@SuppressWarnings("unchecked")
    public <T extends SSWAPNode> T translate(T node) {
		if (clientSideTranslation) {
			// for client side translation we recompute the results every time 
			// as opposed to caching them in server-side translation
			// (RIG's subjects are unlikely to be modified on the server side)
			
			try {
				// perform validation/mapping/translation now
				persist();
	            validateAgainstRDG(rdg);
            }
            catch (ValidationException e) {
            	e.printStackTrace();
            	// NOTE: maybe we should change the signature of translate to throw ValidationException?
            	return node;
            }
		}
		
		if (node.getDocument() != this) {
			// return null for all nodes that do not belong to this RIG
			return null;
		}

		T result = (T) translatedNodes.get(node);

		if (result == null) {
			result = node;
		}
			
		return result;				
	}
		
	/**
	 * @inheritDoc
	 */
	public RRGResponse invoke() {
		return doInvoke(null /* timeout */);
	}
	
	/**
	 * @inheritDoc
	 */
	public RRGResponse invoke(long timeout) {
		return doInvoke(timeout);
	}

	/**
	 * Invokes the service with this RIG and the specified connect/read timeout (if provided).
	 * 
	 * Always returns a RRGResonse object, even on failure. Inspection of the response contains
	 * information on failure. Response exception objects are generated according to:
	 * 
	 * IOException if an I/O error occurred while communicating with the service.
	 * DataAccessException if the RRG returned by the response was not syntactically valid
	 * ValidationException if the RRG returned by the response was not semantically valid.
	 * 
	 * @param timeout connect/read timeout in milliseconds (may be null; in such a case the default timeout 
	 * will be used as specified in Config's info.sswap.impl.empire.model.RIG_INVOCATION_TIMEOUT parameter).
	 * @return RRGResponse if an HTTP response was received
	 */
	private RRGResponse doInvoke(Long timeout) {
		
		// multiple embedded returns; error status is contained in RRGResponse object
		
		if (getResource().isAnonymous()) {
			IOException ioe = new IOException("Cannot invoke because SSWAP Resource has no URL");
			return (new HTTPAPIImpl()).new RRGResponse(null,null,ioe);
		}
		
		try {
			validate();
		} catch ( ValidationException ve ) {
			ve = new ValidationException("RIG validation error: " + ve.getMessage());
			return (new HTTPAPIImpl()).new RRGResponse(null,null,ve);
		}
		
		// serialize RIG
		ByteArrayOutputStream bos = new ByteArrayOutputStream();		
		serialize(bos, RDFRepresentation.RDF_XML, false /* commentedOutput */);
		
		// set up variables for storing invocation result
		RRG rrg = null;
		Response response = null;

		// service being invoked
		String serviceURIStr = getURI().toString();
						
		// invoke and read results
		try {
			if (timeout != null) {
				response = ModelUtils.invoke(getURI(), new ByteArrayInputStream(bos.toByteArray()), true /* returnOnHTTPError */, timeout);
			}
			else {
				response = ModelUtils.invoke(getURI(), new ByteArrayInputStream(bos.toByteArray()), true /* returnOnHTTPError */);
			}
			
			if ( response == null ) {
				throw new IOException();
			}
		} catch ( IOException ioe ) {
			ioe = new IOException("Error invoking service at: " + serviceURIStr + "; " + ioe.getMessage());
			return (new HTTPAPIImpl()).new RRGResponse(null,response,ioe);
		}
					
		int responseCode = response.getResponseCode();
		if ( responseCode == HttpServletResponse.SC_BAD_REQUEST ) {
			StringBuffer message = new StringBuffer("Invalid request for service at: " + serviceURIStr);
			
			if (response.getHeader(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER) != null) {
				message.append("; ");
				message.append(response.getHeader(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER));
			}
			
			IOException ioe = new IOException(message.toString());
			return (new HTTPAPIImpl()).new RRGResponse(null,response,ioe);
			
		} else if ( responseCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE ) {
			
			IOException ioe = new IOException(response.getMessage() + " from: " + serviceURIStr);
			return (new HTTPAPIImpl()).new RRGResponse(null,response,ioe);
		}
		
		InputStream inputStream = response.getContent();
			
		if ( inputStream == null ) {
			DataAccessException dae = new DataAccessException("No content returned from: " + serviceURIStr + "; [" + response.getResponseCode() + "] " + response.getMessage());
			return (new HTTPAPIImpl()).new RRGResponse(null,response,dae);
		}
		
		try {
			if ( (rrg = getRRG(inputStream)) == null ) {
				DataAccessException dae = new DataAccessException("");  // getRRG() may throw its own DataAccessException
				return (new HTTPAPIImpl()).new RRGResponse(null,response,dae);
			}
		} catch ( DataAccessException dae ) {
			dae = new DataAccessException("Could not parse RRG from: " + serviceURIStr + "; " + dae.getMessage());
			return (new HTTPAPIImpl()).new RRGResponse(null,response,dae);
		}

		try {
			rrg.validate();
		} catch ( ValidationException ve ) {
			ve = new ValidationException("RRG validation error from: " + serviceURIStr + "; "+ ve.getMessage());
			return (new HTTPAPIImpl()).new RRGResponse(null,response,ve);
		}
		
		RRGResponse rrgResponse = null;
		try {
				HTTPAPIImpl httpAPIImpl = new HTTPAPIImpl();
				rrgResponse = httpAPIImpl.new RRGResponse(rrg,response);  // will close response on non-null rrg
		} catch ( Exception e ) { // rare
			IOException ioe = new IOException("Failed to build an RRG on successful invocation of: " + serviceURIStr + "; "+ e.getMessage());
			return (new HTTPAPIImpl()).new RRGResponse(null,response,ioe);
		}
		
		return rrgResponse;
	}
	
	public String getGraphType() {
		return "RIG";
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPResource getTranslatedResource() {
		return translate(getResource());
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPSubject> getTranslatedSubjects() {
		List<SSWAPSubject> translatedSubjects = new LinkedList<SSWAPSubject>();		
		
		for (SSWAPGraph graph : getMatchingGraphs()) {
			for (SSWAPSubject subject : graph.getSubjects()) {
				if (getTranslationMap().containsKey(subject)) {				
					translatedSubjects.add(translate(subject));
				}
			}
		}
		
		return translatedSubjects;
	}	
}
