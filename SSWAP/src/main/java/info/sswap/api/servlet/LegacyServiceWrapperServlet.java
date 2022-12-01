/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.clarkparsia.utils.web.Response;

import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.model.ImplFactory;
import info.sswap.impl.empire.model.IndividualImpl;
import info.sswap.impl.empire.model.ModelUtils;
import info.sswap.impl.empire.model.SourceModel;

public class LegacyServiceWrapperServlet extends AbstractSSWAPServlet {

	private static final long serialVersionUID = 6176407187653076879L;
	
	/**
	 * Interface to Logging API
	 */
	private static final Logger LOGGER = LogManager.getLogger(AbstractSSWAPServlet.class);

	private static final String PROXY_SERVICE_URL_PARAM = "ProxyServiceURL";
	private static final String MAX_OBJECTS_PARAM = "MaxObjects";
	
	private String proxyServiceURL;
	private int maxObjects;
	
	public synchronized void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		proxyServiceURL = servletConfig.getInitParameter(PROXY_SERVICE_URL_PARAM);
		
		if (proxyServiceURL == null) {
			throw new ServletException("Required parameter missing in web.xml: " + PROXY_SERVICE_URL_PARAM);
		}
		
		if (servletConfig.getInitParameter(MAX_OBJECTS_PARAM) != null) {
			try {
				maxObjects = Integer.parseInt(servletConfig.getInitParameter(MAX_OBJECTS_PARAM));
			}
			catch (NumberFormatException e) {
				throw new ServletException(String.format("Invalid value in web.xml for %s: " + servletConfig.getInitParameter(MAX_OBJECTS_PARAM), MAX_OBJECTS_PARAM));	
			}
		}
		else {
			maxObjects = -1;
		}
	}
	
	@Override
	protected void handleRequest(RIG rig) {
		ExtensionAPI.setValueValidation(rig, false /* enabled */);
		
		RDG backendRDG = SSWAP.getRDG(URI.create(proxyServiceURL));
		
		// prepare RIG for the back service, based on the current RIG
		RIG backendRIG = backendRDG.getRIG();
		ExtensionAPI.setValueValidation(backendRIG, false /* enabled */);
		
		Set<SSWAPGraph> matchingGraphs = new HashSet<SSWAPGraph>();
		
		for (SSWAPSubject matchingSubject : rig.getTranslatedSubjects()) {
			matchingGraphs.add(matchingSubject.getGraph());
		}
		
		List<SSWAPGraph> backendGraphs = new LinkedList<SSWAPGraph>();		
		
		for (SSWAPGraph graph : matchingGraphs) {
			SSWAPGraph backendGraph = backendRIG.createGraph();
			
			List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
			
			for (SSWAPSubject subject: graph.getSubjects()) {
				subjects.add(copySubject(backendRIG, subject));
			}
			
			backendGraph.setSubjects(subjects);			
			backendGraphs.add(backendGraph);
		}
		
		backendRIG.getResource().setGraphs(backendGraphs);
		
		RRG rrg = null;
		
		// invoke the wrapped service
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Sending RIG to backend service");
				logDebug(backendRIG);
			}
	        rrg = invoke(backendRIG);
        }
        catch (Exception e) {
        	LOGGER.error("Proxy call failed. Returning original RIG to the user", e);
        	return;
        }
        
        if (rrg == null) {
        	LOGGER.error("Proxy call failed. Returning original RIG to the user");
        	return;
        }
        
        if (LOGGER.isDebugEnabled()) {
        	LOGGER.debug("Received RRG from backend service");
        	logDebug(rrg);
        }

        // rewrite the response back
        List<SSWAPGraph> frontendGraphs = new LinkedList<SSWAPGraph>();
		
		for (SSWAPGraph graph : rrg.getResource().getGraphs()) {
			SSWAPGraph frontendGraph = rig.createGraph();
			
			List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
			
			for (SSWAPSubject subject: graph.getSubjects()) {
				subjects.add(copySubject(rig, subject));
			}
			
			frontendGraph.setSubjects(subjects);
			frontendGraphs.add(frontendGraph);
		}
		
		rig.getResource().setGraphs(frontendGraphs);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Final RRG");
			logDebug(rig);
		}
	}
	
	private RRG invoke(RIG rig) throws Exception {
		// manual building of the invocation to mimic the legacy protocol
		
		// serialize RIG
		ByteArrayOutputStream bos = new ByteArrayOutputStream();		
		rig.serialize(bos, RDFRepresentation.RDF_XML, false /* commentedOutput */);
		
		// append the word "graph=" at the beginning and URL encode the RIG
		byte[] rigBytesLegacy = ("graph=" + URLEncoder.encode(new String(bos.toByteArray()), "UTF-8")).getBytes();
				
		// send the POST request and obtain RRG
		RRG rrg = null;
		Response response = null;
		try {
			response = ModelUtils.invoke(rig.getURI(),rigBytesLegacy);
			rrg = rig.getRRG(response.getContent());			
		} finally {
				if ( response != null ) {
					response.close();
				}
		}
		
		return rrg;
	}
	
	private SSWAPSubject copySubject(RIG dstRIG, SSWAPSubject subject) {
		SSWAPSubject result = null;
		
		if (subject.isAnonymous()) {
			result = dstRIG.createSubject();
		}
		else {
			result = dstRIG.createSubject(subject.getURI());
		}
		
		ImplFactory.get().deepCopyIndividual((SourceModel) dstRIG, subject, (IndividualImpl) result, new HashSet<URI>());
		
		List<SSWAPObject> objects = new LinkedList<SSWAPObject>();
		
		int objectCount = 0;
		for (SSWAPObject object : subject.getObjects()) {
			if (maxObjects != -1 && objectCount >= maxObjects) {			
				break;
			}
			
			objects.add(copyObject(dstRIG, object));
			objectCount++;
		}
		
		result.setObjects(objects);		
		
		return result;		
	}
	
	private SSWAPObject copyObject(RIG dstRIG, SSWAPObject object) {
		SSWAPObject result = null;
		
		if (object.isAnonymous()) {
			result = dstRIG.createObject();
		}
		else {
			result = dstRIG.createObject(object.getURI());
		}
		
		ImplFactory.get().deepCopyIndividual((SourceModel) dstRIG, object, (IndividualImpl) result, new HashSet<URI>());
		
		return result;		
	}
	
	private static void logDebug(SSWAPModel model) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		model.serialize(bos);
		LOGGER.debug(new String(bos.toByteArray()));
	}

}
