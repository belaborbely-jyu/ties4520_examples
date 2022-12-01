/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import info.sswap.api.spi.APIProvider;
import info.sswap.impl.empire.APIProviderImpl;

import java.io.InputStream;
import java.net.URI;

/**
 * Main factory for the creation of SSWAP objects (most elements in the API are
 * just interfaces).
 * <p>
 * Note about naming of the methods: all the <code>create*()</code> methods
 * create objects without accessing the Internet or dereferencing URIs;
 * <i>e.g.</i>, the <code>createRDG</code> method creates a new Resource
 * Description Graph ({@link RDG}) ready to be hosted on the web. All
 * <code>get*()</code> methods attempt to dereference argument URIs as
 * appropriate and return objects that reflect the existing representation on
 * the web. For example, the <code>getRDG</code> method attempts to return an
 * object that corresponds to an <code>RDG</code> hosted at its URI. In case the
 * underlying data does not exist, the behavior of a <code>get*()</code> method
 * depends on the specific method. Methods like <code>getRDG</code> or
 * <code>getPDG</code> will always return an object if possible (if
 * undereferenced and there is no underlying data a later
 * <code>dereference()</code> will fail).
 * <p>
 * Most methods will throw a runtime <code>DataAccessException</code> if the
 * requested class cannot be instantiated, for example due to an I/O or parsing
 * error. As in all Java, other runtime exceptions are possible. Common practice
 * is to catch all runtime exceptions in an outermost main program try/catch
 * block to handle unexpected runtime errors.
 * <p>
 * Lower-level objects are created by their super class, <i>e.g.</i>, see
 * <code>SSWAPDocument</code>.
 * 
 * @see SSWAPProtocol
 * @see SSWAPDocument
 * @see DataAccessException
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public class SSWAP {
	/**
	 * The current provider of the API. The actual creation of the objects is delegated to that provider. (This allows
	 * the API to be pluggable; that is, to easily switch the implementations.) As a general rule, the objects created
	 * by two different providers should not be mixed together (e.g., a SSWAPProvider created by one provider should not
	 * be added to a PDG created by another provider).
	 */
	private static APIProvider apiProvider = APIProviderImpl.get();

	/**
	 * Creates a new RDG
	 * 
	 * @param resourceURI
	 *            the URI of the SSWAPResource (and the RDG itself)
	 * @param name
	 *            the name of the resource
	 * @param oneLineDescription
	 *            the one line description of the resource
	 * @param providerURI
	 *            the URI of the provider
	 * @return the newly created RDG object
	 * @throws DataAccessException runtime error if the class cannot be created
	 */
	public static RDG createRDG(URI resourceURI, String name, String oneLineDescription, URI providerURI) throws DataAccessException {
		return apiProvider.createRDG(resourceURI, name, oneLineDescription, providerURI);
	}

	/**
	 * Creates a new RDG
	 * 
	 * @param resourceURI
	 *            the URI of the SSWAPResource (and the RDG itself)
	 * @param name
	 *            the name of the resource
	 * @param oneLineDescription
	 *            the one line description of the resource
	 * @param provider
	 *            the SSWAPProvider for this RDG
	 * @return the newly created RDG object
	 * @throws DataAccessException runtime error if the class cannot be created
	 */
	public static RDG createRDG(URI resourceURI, String name, String oneLineDescription, SSWAPProvider provider) throws DataAccessException {
		return apiProvider.createRDG(resourceURI, name, oneLineDescription, provider.getURI());
	}

	/**
	 * Gets a SSWAPResource that is defined in the RDG that exists at the specified URI (the resource
	 * should have the same URI as the RDG).
	 * 
	 * @param resourceURI the URI of the resource (and the containing RDG)
	 * @return the SSWAPResource 
	 * @throws DataAccessException runtime error if the class cannot be created
	 */
	public static SSWAPResource createResource(URI resourceURI) throws DataAccessException {
		return apiProvider.createResource(resourceURI);
	}
	
	/**
	 * Creates an object representing an RDG. The returned object is dereferenced.
	 * 
	 * @param uri the URI of the RDG
	 * @return an RDG that is not dereferenced
	 * @throws DataAccessException runtime error if the class cannot be created
	 */
	public static RDG getRDG(URI uri) throws DataAccessException {
		return apiProvider.getRDG(uri);
	}

	/**
	 * Reads an RQG from an input stream.
	 * 
	 * @param is the input stream containing the representation of the RQG
	 * @return the RQG
	 * @throws DataAccessException runtime error if the class cannot be created (e.g., I/O error or malformed data that cannot be parsed)
	 */
	public static RQG getRQG(InputStream is) throws DataAccessException {
		return apiProvider.getRQG(is);
	}
	
	/**
	 * Creates a basic RQG from scratch. The created RQG contains only SSWAPResource.
	 * 
	 * @param resourceURI the URI of the resource in the RQG; may be null for anonymous resources
	 * @throws DataAccessException runtime error if the class cannot be created
	 * @return the RQG with a SSWAPResource
	 */
	public static RQG createRQG(URI resourceURI) throws DataAccessException {
		return apiProvider.createRQG(resourceURI);
	}
	
	/**
	 * Creates a SSWAPDocument graph from its serialization in an input stream.
	 * The <code>SSWAPResource</code> will be a blank node. This is usually
	 * inappropriate for anything other than a <code>RQG</code>; for other
	 * graphs see <code>getResourceGraph(InputStream, Class, URI)</code>.
	 * 
	 * @param <T>
	 *            the template parameter that specifies the type of
	 *            SSWAPDocument graph to be created (e.g., RQG).
	 * @param is
	 *            the input stream from which the contents of the graph should
	 *            be read
	 * @param clazz
	 *            the Java Class object that identifies the type of
	 *            SSWAPDocument graph (and provides the instantiation for the
	 *            template parameter; e.g., RQG.class)
	 * @return the created SSWAPDocument graph
	 * @throws DataAccessException
	 *             runtime error if the class cannot be created (including I/O
	 *             error or malformed data that cannot be parsed)
	 * @see #getResourceGraph(InputStream, Class, URI)
	 */
	public static <T extends SSWAPDocument> T getResourceGraph(InputStream is, Class<T> clazz) throws DataAccessException {
		return getResourceGraph(is, clazz, null);
	}
	
	/**
	 * Creates a SSWAPDocument graph from its serialization in an input stream.
	 * The <code>SSWAPResource</code> is set to the URI argument.
	 * 
	 * 
	 * 
	 * @param <T>
	 *            the template parameter that specifies the type of
	 *            SSWAPDocument graph to be created (<i>e.g.</i>,
	 *            <code>PDG<code>, <code>RDG<code>, RIG<code>, or <code>RRG<code>.
.
	 * @param is
	 *            the input stream from which the contents of the graph should
	 *            be read
	 * @param clazz
	 *            the Java Class object that identifies the type of
	 *            SSWAPDocument graph (and provides the instantiation for the
	 *            template parameter; <i>e.g.</i>, RDG.class, RIG.class, etc.)
	 * @param uri
	 *            the URI to dereference to obtain the graph
	 * @return the created SSWAPDocument graph
	 * @throws DataAccessException
	 *             runtime error if the class cannot be created (including I/O
	 *             error or malformed data that cannot be parsed)
	 * 
	 */
	public static <T extends SSWAPDocument> T getResourceGraph(InputStream is, Class<T> clazz, URI uri) throws DataAccessException {
		return apiProvider.getResourceGraph(is, clazz, uri);
	}
	
	/**
	 * Creates an object representing an PDG. Initially, the returned object is not dereferenced, and it contains only
	 * the URI. Actual retrieval of the contents in the PDG occurs, when dereference() method is called.
	 * 
	 * @param providerURI
	 *            the URI of the PDG
	 * @throws DataAccessException runtime error if the class cannot be created
	 * @return a PDG that is not dereferenced
	 */
	public static PDG getPDG(URI providerURI) throws DataAccessException {
		return apiProvider.getPDG(providerURI);
	}
	

	/**
	 * Creates a PDG with the specified values. 
	 * 
	 * @param providerURI the URI of the provider
	 * @param name the name of the provider
	 * @param oneLineDescription a one-line description of the PDG
	 * @throws DataAccessException runtime error if the class cannot be created
	 * @return the created PDG
	 */
	public static PDG createPDG(URI providerURI, String name, String oneLineDescription) throws DataAccessException {
		return apiProvider.createPDG(providerURI, name, oneLineDescription);
	}

	/**
	 * Creates an object representing a SSWAPProvider. Initially, the returned object is not dereferenced, and it
	 * contains only the URI. When the dereference() method is called on this SSWAPProvider, it will retrieve the
	 * contents of the PDG for that SSWAPProvider, and populate the object with the provider data.
	 * 
	 * @param providerURI
	 *            URI of the SSWAPProvider (and at the same time of the PDG for that SSWAPProvider)
	 * @return an SSWAPProvider that is not dereferenced
	 * @throws DataAccessException runtime error if the class cannot be created
	 */
	public static SSWAPProvider createProvider(URI providerURI) throws DataAccessException {
		return apiProvider.createProvider(providerURI);
	}

	/**
	 * Creates an empty SSWAPDocument. This method can be used to contain other SSWAPDocuments if it is undesirable to create
	 * a full protocol graph or a PDG.
	 * @throws DataAccessException runtime error if the class cannot be created
	 * @return the newly created SSWAPDocument
	 */
	public static SSWAPDocument createSSWAPDocument() throws DataAccessException {
		return apiProvider.createSSWAPDocument(null);
	}
	
	/**
	 * Creates an empty SSWAPModel. This method can be used to contain other SSWAPModels if it is undesirable to create
	 * a full protocol graph or a PDG.
	 * 
	 * @param uri
	 *            the URI of the SSWAP model
	 * @return an empty SSWAP model
	 * @throws DataAccessException runtime error if the class cannot be created
	 */
	public static SSWAPDocument createSSWAPDocument(URI uri) throws DataAccessException {
		return apiProvider.createSSWAPDocument(uri);
	}
	
	public static VersionInformation getVersionInformation() {
		return VersionInformation.get();
	}
	
	/**
	 * Gets a cache used to store ontology terms that were retrieved from the network. This method can be used to 
	 * control cache features like the time-to-live of entries in the cache or clearing the cache. 
	 * 
	 * @return the cache for ontology terms
	 */
	public static Cache getCache() {
		return apiProvider.getCache();
	}
}
