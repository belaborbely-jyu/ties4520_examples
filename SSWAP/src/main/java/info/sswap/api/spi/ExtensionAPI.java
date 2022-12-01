/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.spi;

import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.impl.empire.APIProviderImpl;

import java.net.URI;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * An API for extensions of the basic SSWAP API. These are methods primarily for
 * advanced users to aid in debugging. The methods in this API may not be
 * be implemented by all API providers.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ExtensionAPI {
	/**
	 * The current provider of the API. The actual creation of the objects is delegated to that provider. (This allows
	 * the API to be pluggable; that is, to easily switch the implementations.) As a general rule, the objects created
	 * by two different providers should not be mixed together (e.g., a SSWAPProvider created by one provider should not
	 * be added to a PDG created by another provider).
	 */
	private static APIProvider apiProvider = APIProviderImpl.get();

	/**
	 * Gets the document that contains the TBox of this document (including both inferred and asserted statements).   
	 * @param document The source document from which to get the inferred TBox
	 * 
	 * @return the document with the TBox
	 */

	public static SSWAPDocument getInferredTBox(SSWAPDocument document) {
		return apiProvider.getInferredTBox(document);
	}
	
	/**
	 * Gets the document that contains the ABox of this document (including both inferred and asserted statements).   
	 * @param document The source document from which to get the inferred ABox
	 * 
	 * @return the document with the ABox
	 */
	public static SSWAPDocument getInferredABox(SSWAPDocument document) {
		return apiProvider.getInferredABox(document);
	}
	
	/**
	 * Gets the SSWAPDocument that contains the closure of the specified document.
	 * @param document the closure document
	 * @return the closure document
	 */
	public static SSWAPDocument getClosureDocument(SSWAPDocument document) {
		return apiProvider.getClosureDocument(document);
	}
	
	/**
	 * Gets the SSWAPDocument that contains all the inferred facts for the specified document
	 * (and subsequently it also contains its closure, which is required to compute the inferred facts).
	 * 
	 * NOTE: In certain situations the number of inferred facts may be very large. Use this method
	 * with caution.
	 * 
	 * @param document the documents for which all the inferred facts should be retrieved
	 * @return the document with all inferred facts
	 */
	public static SSWAPDocument getInferredDocument(SSWAPDocument document) {
		return apiProvider.getInferredDocument(document);
	}
	
	/**
	 * Returns the representation of the given SSWAPModel using Jena interface   
	 * 
	 * @param model the SSWAPModel
	 * @return Jena model that contains the representation of this document
	 */
	public static Model asJenaModel(SSWAPModel model) {
		return apiProvider.asJenaModel(model);
	}
	
	/**
	 * Creates a SSWAP document from the specified Jena model. 
	 * @param <T> template parameter to specify the desired document type
	 * @param model Jena model that will be the source of triples for the document 
	 * @param clazz the name of the interface (e.g., SSWAPDocument.class or RDG.class) for the requested document
	 * @return the created document
	 */
	public static <T extends SSWAPDocument> T createDocument(Model model, Class<T> clazz) {
		return apiProvider.createDocument(model, clazz);
	}
	
	/**
	 * Creates a SSWAP document from the specified Jena model with the specified URI 
	 * @param <T> template parameter to specify the desired document type
	 * @param model Jena model that will be the source of triples for the document 
	 * @param clazz the name of the interface (e.g., SSWAPDocument.class or RDG.class) for the requested document
	 * @param uri the URI of the generated document (matters only for SSWAPProtocol documents)
	 * @return the created document
	 */
	public static <T extends SSWAPDocument> T createDocument(Model model, Class<T> clazz, URI uri) {
		return apiProvider.createDocument(model, clazz, uri);
	}

	/**
	 * Sets a model-specific byte limit to retrieve the closure
	 * 
	 * @param model the model for which the limit should be set
	 * @param byteLimit the maximum number of bytes to retrieve the closure, or -1 to remove any 
	 * previous model-specific limit (a system-wide limit will be used for this model)
	 * @throws UnsupportedOperationException if the operation is not supported by this API Implementation
	 */
    public static void setMaxClosureBytes(SSWAPModel model, long byteLimit) throws UnsupportedOperationException {
		apiProvider.setMaxClosureBytes(model, byteLimit);
    }

    /**
	 * Sets a model-specific limit on the number of threads used to retrieve the closure
	 * 
	 * @param model the model for which the limit should be set
	 * @param threads the number of threads, or -1 to remove any previous model-specific limit 
	 * (a system-wide limit will be used for this model)
	 * @throws UnsupportedOperationException if the operation is not supported by this API Implementation
	 */
    public static void setMaxClosureThreads(SSWAPModel model, int threads) throws UnsupportedOperationException {
    	apiProvider.setMaxClosureThreads(model, threads);
	}

    /**
	 * Sets a model-specific time limit to retrieve the closure
	 * 
	 * @param model the model for which the limit should be set
	 * @param timeLimit the maximum number of milliseconds to retrieve the closure, or -1 to remove any 
	 * previous model-specific limit (a system-wide limit will be used for this model)
	 * @throws UnsupportedOperationException if the operation is not supported by this API Implementation
	 */
    public static void setMaxClosureTime(SSWAPModel model, long timeLimit) throws UnsupportedOperationException {
    	apiProvider.setMaxClosureTime(model, timeLimit);
	}
    
    /**
     * Sets the syntax in which explanations will be provided, if reasoning service finds an inconsistent ontology.
     * The legal values for the syntax are RDF/XML, RDF/XML-ABBREV, TURTLE, and PELLET (for standard Pellet explanations). 
     * 
     * @param explanationSyntax the desired syntax
     * @throws UnsupportedOperationException if the operation is not supported by this API implementation
     */
    public static void setExplanationSyntax(String explanationSyntax) throws UnsupportedOperationException {
    	apiProvider.setExplanationSyntax(explanationSyntax);
    }
    
    public static RQG generateRQG(RDG upstreamService, RDG downstreamService) throws UnsupportedOperationException {
    	return apiProvider.generateRQG(upstreamService, downstreamService);
    }
    
    public static RQG generateRQG(RDG upstreamService, RDG downstreamService, URI resultURI) throws UnsupportedOperationException {
    	return apiProvider.generateRQG(upstreamService, downstreamService, resultURI);
    }
    
    public static RIG getAsyncRIG(URI serviceURI, URI upstreamRRG) throws UnsupportedOperationException {
    	return apiProvider.getAsyncRIG(serviceURI, upstreamRRG);
    }
    
    /**
     * Performs a deep copy of an element, possibly to another document.
     * 
     * @param dstDocument the document where the copy should be placed.
     * @param element the element to be copied.
     * @return the copy
     * @throws UnsupportedOperationException if the operation is not supported by this API implementation
     */
    public static SSWAPElement copyElement(SSWAPDocument dstDocument, SSWAPElement element) throws UnsupportedOperationException {
    	return apiProvider.copyElement(dstDocument, element);
    }
    
    /**
     * Turns on/off value validation performed when setting values of properties for SSWAPIndividual. 
     *  
     * @param document the document in which the validation should be turned on/off
     * @param enabled true if the validation should be turned on, false, if the validation should be turned off
     * @throws UnsupportedOperationException
     */
    public static void setValueValidation(SSWAPDocument document, boolean enabled) throws UnsupportedOperationException {
    	apiProvider.setValueValidation(document, enabled);
    }
    

    /**
     * Sets flag whether this source model will perform the closure to deliver terms to its reasoning service, or whether
	 * the reasoning service should solely rely on terms in this source model. Changing this flag has only effect
	 * if the reasoning service has not yet been initialized.  
     *  
     * @param document the document in which the closure would be turned on or off
     * @param enabled true if the closure computation should be turned on, false, if the closure computation should be turned off
     * @throws UnsupportedOperationException
     */
    public static void setClosureEnabled(SSWAPDocument document, boolean enabled) throws UnsupportedOperationException {
    	apiProvider.setClosureEnabled(document, enabled);
    }
    
    public static Collection<String> getInferredTypeURIs(SSWAPIndividual individual) throws UnsupportedOperationException {
    	return apiProvider.getInferredTypeURIs(individual);
    }
    
    public static RDG createCompositeService(URI serviceURI, String name, String description, URI providerURI, RDG firstService, RDG lastService) throws UnsupportedOperationException {
    	return apiProvider.createCompositeService(serviceURI, name, description, providerURI, firstService, lastService);
    }
    
	/**
	 * Returns a copy of the RQG with all <code>SSWAPObject</code>s stripped of declared types and properties.
	 * 
	 * @param rqg source <code>RQG</code>
	 * @return copy <code>RQG</code> with <code>SSWAPObject</code> declared types and properties removed
	 * @throws UnsupportedOperationException
	 * @see SSWAPIndividual#removeProperty(info.sswap.api.model.SSWAPProperty)
	 * @see SSWAPIndividual#removeType(info.sswap.api.model.SSWAPType)
	 */
    // coordinate these javadocs with entry in APIProvider
    public static RQG inputOnlyRQG(RQG rqg) throws UnsupportedOperationException {
    	return apiProvider.inputOnlyRQG(rqg);
    }
    
    /**
	 * Returns a copy of the RQG with all <code>SSWAPSubject</code>s stripped of declared types and properties.
	 * 
	 * @param rqg source <code>RQG</code>
	 * @return copy <code>RQG</code> with <code>SSWAPSubject</code> declared types and properties removed
	 * @throws UnsupportedOperationException
	 * @see SSWAPIndividual#removeProperty(info.sswap.api.model.SSWAPProperty)
	 * @see SSWAPIndividual#removeType(info.sswap.api.model.SSWAPType)
	 */
    // coordinate these javadocs with entry in APIProvider
    public static RQG outputOnlyRQG(RQG rqg) throws UnsupportedOperationException {
    	return apiProvider.outputOnlyRQG(rqg);
    }
    
    public static boolean isUnrestricted(RQG rqg) throws UnsupportedOperationException {
    	return apiProvider.isUnrestricted(rqg);
    }
    
    /**
     * Return the source node (<code>SSWAPResource</code> or <code>SSWAPSubject</code>)
     * from which the <code>translatedNode</code> was derived. If the <code>translatedNode</code> was not
     * actually translated (<i>i.e.</i> is an untranslated <code>SSWAPNode</code>), then it itself is returned.
     * 
     * @param <T> {@link SSWAPResource} or {@link SSWAPSubject}
     * @param translatedNode translated individual for which its source is sought
     * @return individual that was the source of the translation (<i>e.g.</i>, <code>RIG</code> <code>SSWAPResource</code>
     * 	or <code>SSWAPSubject</code>)
     * @throws UnsupportedOperationException upon an error retrieving the translation from the API provider
     * @see RIG#translate(SSWAPNode)
     */
    public static <T extends SSWAPNode> T getUntranslatedNode(T translatedNode) throws UnsupportedOperationException {
    	return apiProvider.getUntranslatedNode(translatedNode);
    }
}
