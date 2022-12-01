/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.spi;

import info.sswap.api.model.Cache;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.PDG;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * The interface implemented by the providers of the API. The methods of this interface create actual implementations of
 * SSWAP interfaces.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface APIProvider {
	/**
	 * Gets an undereferenced PDG implementation.
	 * 
	 * @param uri
	 *            URI of the PDG
	 * @return the implementation of the PDG
	 */
	public PDG getPDG(URI uri);

	/**
	 * Gets an undereferenced SSWAPProvider implementation
	 * 
	 * @param uri
	 *            URI of the provider
	 * @return the implementation of the SSWAPProvider
	 */
	public SSWAPProvider createProvider(URI uri);

	/**
	 * Gets a dereferenced RDG implementation
	 * 
	 * @param uri
	 *            URI of the RDG
	 * @throws DataAccessException if an error should occur while trying to access RDG's data
	 * @return the implementation of the RDG 
	 */
	public RDG getRDG(URI uri) throws DataAccessException;
	
	/**
	 * Gets an undereferenced SSWAPResource implementation
	 * 
	 * @param uri
	 *            URI of the Resource
	 * @return the implementation of the resource
	 */
	public SSWAPResource createResource(URI uri);

	/**
	 * Creates a new RDG object.
	 * 
	 * @param resourceURI
	 *            the URI of the SSWAP Resource (and the RDG itself)
	 * @param name
	 *            the name of the service described in this RDG
	 * @param oneLineDescription
	 *            a short description of the RDG
	 * @param providerURI
	 *            an URI of the provider of this service
	 * @return the created RDG
	 */
	public RDG createRDG(URI resourceURI, String name, String oneLineDescription, URI providerURI);
	
	/**
	 * Reads an RQG from an input stream.
	 * 
	 * @param is the input stream containing the representation of the RQG
	 * @return the RQG
	 */
	public RQG getRQG(InputStream is);
	
	/**
	 * Creates a basic RQG from scratch. The created RQG contains only SSWAPResource.
	 * 
	 * @param resourceURI the URI of the resource in the RQG; may be null for anonymous resources
	 * @return the RQG with a SSWAPResource
	 */
	public RQG createRQG(URI resourceURI);
	
	/**
	 * Creates a SSWAPDocument graph from its serialization in an input stream.
	 * 
	 * @param <T> the template parameter that specifies the type of SSWAPDocument graph to be created (e.g., RDG, RIG, etc.).
	 * @param is the input stream from which the contents of the graph should be read
	 * @param clazz the Java Class object that identifies the type of SSWAPDocument graph (and provides the instantiation
	 * for the template parameter; e.g., RDG.class, RIG.class, etc.)
	 * @param uri the URI to dereference to obtain the graph
	 * @return the created SSWAPDocument graph
	 * @throws DataAccessException on parsing or other data error
	 */
	public <T extends SSWAPDocument> T getResourceGraph(InputStream is, Class<T> clazz, URI uri) throws DataAccessException;
	
	/**
	 * Creates a new PDG (Provider Description Graph).
	 * 
	 * @param providerURI the URI of the SSWAP Provider (and the PDG itself)
	 * @param name the name of the provider described in this PDG
	 * @param oneLineDescription the one line description of the provider
	 * @return newly created PDG
	 */
	public PDG createPDG(URI providerURI, String name, String oneLineDescription);

	/**
	 * Creates an empty SSWAPModel. This method can be used to contain other SSWAPModels if it is undesirable to create
	 * a full SSWAPCanonicalGraph or a PDG.
	 * 
	 * @param uri
	 *            the URI of the SSWAP model
	 * @return an empty SSWAP model
	 */
	public SSWAPDocument createSSWAPDocument(URI uri);
	
	// optional methods for a provider
	
	/**
	 * Gets the document that contains the TBox of this document (including both inferred and asserted statements).   
	 * 
	 * @param document source SSWAPDocument
	 * @throws UnsupportedOperationException if the specified provider does not support this feature
	 * @return the document with the TBox
	 */
	public SSWAPDocument getInferredTBox(SSWAPDocument document) throws UnsupportedOperationException;
	
	/**
	 * Gets the document that contains the ABox of this document (including both inferred and asserted statements).   
	 * 
	 * @param document source SSWAPDocument
	 * @throws UnsupportedOperationException if the specified provider does not support this feature
	 * @return the document with the ABox
	 */
	public SSWAPDocument getInferredABox(SSWAPDocument document) throws UnsupportedOperationException;
	
	/**
	 * Returns the representation of the given SSWAPModel using Jena interface   
	 * 
	 * @param model the SSWAPModel
	 * @throws UnsupportedOperationException if the specified provider does not support this feature
	 * @return Jena model that contains the representation of this document
	 */
	public Model asJenaModel(SSWAPModel model) throws UnsupportedOperationException;
	
	/**
	 * Sets a model-specific limit on the number of threads used to retrieve the closure
	 * 
	 * @param model the model for which the limit should be set
	 * @param threads the number of threads, or -1 to remove any previous model-specific limit 
	 * (a system-wide limit will be used for this model)
	 * @throws UnsupportedOperationException if the operation is not supported by this API Implementation
	 */
	public void setMaxClosureThreads(SSWAPModel model, int threads) throws UnsupportedOperationException;

	/**
	 * Sets a model-specific time limit to retrieve the closure
	 * 
	 * @param model the model for which the limit should be set
	 * @param timeLimit the maximum number of milliseconds to retrieve the closure, or -1 to remove any 
	 * previous model-specific limit (a system-wide limit will be used for this model)
	 * @throws UnsupportedOperationException if the operation is not supported by this API Implementation
	 */
	public void setMaxClosureTime(SSWAPModel model, long timeLimit) throws UnsupportedOperationException;
	
	/**
	 * Sets a model-specific byte limit to retrieve the closure
	 * 
	 * @param model the model for which the limit should be set
	 * @param byteLimit the maximum number of bytes to retrieve the closure, or -1 to remove any 
	 * previous model-specific limit (a system-wide limit will be used for this model)
	 * @throws UnsupportedOperationException if the operation is not supported by this API Implementation
	 */
	public void setMaxClosureBytes(SSWAPModel model, long byteLimit) throws UnsupportedOperationException;
	
	public <T extends SSWAPDocument> T createDocument(Model model, Class<T> clazz) throws UnsupportedOperationException;
	
	public <T extends SSWAPDocument> T createDocument(Model model, Class<T> clazz, URI uri) throws UnsupportedOperationException;
	
	public SSWAPDocument getClosureDocument(SSWAPDocument document) throws UnsupportedOperationException;
	
	public SSWAPDocument getInferredDocument(SSWAPDocument document) throws UnsupportedOperationException;
	
	public void setExplanationSyntax(String explanationSyntax) throws UnsupportedOperationException;
	
	public RQG generateRQG(RDG upstreamService, RDG downstreamService) throws UnsupportedOperationException;
	
	public RQG generateRQG(RDG upstreamService, RDG downstreamService, URI resultURI) throws UnsupportedOperationException;

	public RIG getAsyncRIG(URI serviceURI, URI upstreamRRG) throws UnsupportedOperationException;
	
	public SSWAPElement copyElement(SSWAPDocument dstDocument, SSWAPElement element) throws UnsupportedOperationException;
	
	public Cache getCache();
	
	public void setValueValidation(SSWAPDocument document, boolean enabled) throws UnsupportedOperationException;
	
	public void setClosureEnabled(SSWAPDocument document, boolean enabled) throws UnsupportedOperationException;
	
	public Collection<String> getInferredTypeURIs(SSWAPIndividual individual) throws UnsupportedOperationException;
	
	public RDG createCompositeService(URI serviceURI, String name, String description, URI providerURI, RDG firstService, RDG lastService) throws UnsupportedOperationException;
	
	/**
	 * Returns a copy of the RQG with all <code>SSWAPObject</code>s stripped of declared types and properties.
	 * 
	 * @param rqg source <code>RQG</code>
	 * @return copy <code>RQG</code> with <code>SSWAPObject</code> declared types and properties removed
	 * @throws UnsupportedOperationException
	 * @see SSWAPIndividual#removeProperty(info.sswap.api.model.SSWAPProperty)
	 * @see SSWAPIndividual#removeType(info.sswap.api.model.SSWAPType)
	 */
	public RQG inputOnlyRQG(RQG rqg) throws UnsupportedOperationException;
	
	/**
	 * Returns a copy of the RQG with all <code>SSWAPSubject</code>s stripped of declared types and properties.
	 * 
	 * @param rqg source <code>RQG</code>
	 * @return copy <code>RQG</code> with <code>SSWAPSubject</code> declared types and properties removed
	 * @throws UnsupportedOperationException
	 * @see SSWAPIndividual#removeProperty(info.sswap.api.model.SSWAPProperty)
	 * @see SSWAPIndividual#removeType(info.sswap.api.model.SSWAPType)
	 */
	public RQG outputOnlyRQG(RQG rqg) throws UnsupportedOperationException;
	
	public boolean isUnrestricted(RQG rqg) throws UnsupportedOperationException;
	
	public <T extends SSWAPNode> T getUntranslatedNode(T translatedNode) throws UnsupportedOperationException;
}
