/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * The most abstract interface in the SSWAP Java API. Represents any fragment
 * represented by the underlying RDF data (model). When created, a
 * <code>SSWAPModel</code> may represent only the URI to a concept (<i>e.g.</i>,
 * a URI to a resource) rather than the actual RDF data stored at that URI.
 * Such a <code>SSWAPModel</code> is called "not dereferenced" (<i>i.e.</i>, its
 * <code>isDereferenced()</code> method returns false). In order to actually
 * retrieve the data and populate the model, it is necessary to call the
 * <code>dereference()</code> method. This is usually done automatically by the
 * API when it first uses a non-dereferenced model to satisfy a particular task.
 * <p>
 * For objects that are not dereferenced, the only available methods are
 * <code>getURI()</code>, <code>isDereferenced()</code>, and
 * <code>dereference()</code>. All other methods should not be called (there is
 * no data for them yet), and in general, most of them will return null in such
 * a case.
 * <p>
 * It is possible for a SSWAP model to be already dereferenced when created.
 * This typically occurs when an underlying source has already been dereferenced
 * (<i>e.g.</i>, a {@link PDG}), and then a method to retrieve an object within
 * that source of data is called (<i>e.g.</i>, <code>getProvider()</code> to get
 * provider data that is already present within that <code>PDG</code>).
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public interface SSWAPModel {
	/**
	 * Get the URI.
	 * 
	 * For RDGs, RIGs, and RRGs, return the URI of the sswapResource (for RQGs, leave undefined for now). For PDGs
	 * return the URI of the sswapProvider. Return null on failure.
	 * 
	 * @return the URI, such that if the model is serialized and hosted on the web, when dereferenced retrieves the model
	 */
	public URI getURI();

	/**
	 * Returns whether this object represents a dereferenced content stored under the URI, or is it just the URI. For
	 * objects that have not been dereferenced, the only valid methods are getURI(), isDereferenced(), and
	 * dereference().
	 * 
	 * @return true if the object has already been dereferenced
	 */
	public boolean isDereferenced();
	
	/**
	 * Get input stream suitable for reading the serialized model.
	 * 
	 * @return An input stream suitable for reading
	 */
	public InputStream getInputStream();

	/**
	 * Serializes the contents of this object to the specified stream as RDF/XML.
	 * 
	 * @param os
	 *            output stream
	 */
	public void serialize(OutputStream os);

	/**
	 * Serializes the contents of this object to the specified stream.
	 * 
	 * @param os
	 *            output stream
	 * @param representation
	 *            the representation, in which the contents should be written (e.g., RDF/XML).
	 * @param commentedOutput
	 *            true, if the output should contain comments about various standard sections of an RDG (valid only for
	 *            RDF/XML)
	 */
	public void serialize(OutputStream os, RDFRepresentation representation, boolean commentedOutput);

	/**
	 * If the object is not dereferenced, it dereferences it. This involves retrieving the underlying RDF data, and may
	 * result in a network connection.
	 * 
	 * 
	 * @throws IllegalStateException
	 *             if the object has already been dereferenced.
	 * @throws IllegalArgumentException
	 *             if this object does not have an URI, or the URI does not form a valid URL
	 * @throws DataAccessException if an error occurred while trying to read the data (e.g., the data source is unavailable or it is impossible to parse)
	 */
	public void dereference() throws IllegalStateException, IllegalArgumentException, DataAccessException;
	
	/**
	 * Dereferences the object (if it is not yet dereferenced) but it reads the data from the given stream, rather
	 * than trying to to retrieve the URI of this object. (Avoids network connectivity.)
	 * 
	 * @param is the input stream from which the data should be read instead of establishing a network connection
	 * @throws IllegalStateException
	 *            if the object has already been dereferenced.
	 * @throws IllegalArgumentException
	 *            if this object does not have an URI, or the URI does not form a valid URL
	 * @throws DataAccessException if an error occurred while trying to read the data (e.g., the data source is unavailable or it is impossible to parse) 
	 */
	public void dereference(InputStream is) throws IllegalStateException, IllegalArgumentException, DataAccessException;

	/**
	 * For every URI in the model, do successive HTTP GETs and read in the model. Assume RDF/XML. 
	 * Closure obeys rules of self-definition.  Dereferencing is limited by depth and response time, such that
	 * performing a closure results in many HTTP GET calls; the result of which may differ depending on
	 * network conditions and third-party server response.
	 * 
	 * @return the highest (deepest) level of closure achieved.
	 */
	public int doClosure();

	/**
	 * Verifies whether the underlying RDF data conforms to SSWAP syntax and requirements.
	 * 
	 * @throws ValidationException
	 *             if the data violates SSWAP syntax or requirements
	 */
	public void validate() throws ValidationException;

	/**
	 * Checks whether the model fits a particular expressivity profile (e.g., OWL2 DL)
	 * 
	 * @param expressivity expressivity profile
	 * @return true if the model fits the specified expressivity
	 */
	public boolean checkProfile(Expressivity expressivity);
	
	/**
	 * Gets the reasoning service associated with the underlying RDF data
	 * 
	 * @return the reasoning service
	 */
	public ReasoningService getReasoningService();

	/**
	 * Sets a namespace prefix that will be used in serialization of this model.
	 * 
	 * @param prefix
	 *            the prefix
	 * @param uri
	 *            the corresponding URI
	 */
	public void setNsPrefix(String prefix, URI uri);

	/**
	 * Removes a namespace prefix. (The prefixes are used in serialization of this model.)
	 * 
	 * @param prefix
	 *            the prefix to be removed.
	 */
	public void removeNsPrefix(String prefix);

	/**
	 * Gets the map of currently defined namespace prefixes. (These prefixes are used in serialization of this model.)
	 * 
	 * @return the map mapping prefixes to corresponding URIs
	 */
	public Map<String, String> getNsPrefixMap();

	/**
	 * Gets the list of OWL imports in this model (the top-level only).
	 * 
	 * @return a collection of uris of the imports
	 */
	public Collection<String> getImports();

	/**
	 * Adds an import to the list of OWL imports of this model. Adding a URI to the list of imports does not trigger the
	 * import itself (use doClosure()) for that purpose.
	 * 
	 * @param uri
	 *            the URI of the import to be added
	 */
	public void addImport(URI uri);

	/**
	 * Removes an import from the list of imports
	 * 
	 * @param uri
	 *            the import to be removed
	 */
	public void removeImport(URI uri);
	
	/**
	 * Retrieves the document that contains this SSWAPModel
	 *  
	 * @return SSWAPDocument
	 */
	public SSWAPDocument getDocument();
	
	/**
	 * Get the terms that comprise the definition of a URI term. Results may be returned for any URI,
	 * but expected use is for cases where the URI is an OWL class (or type).
	 * 
	 * @param type URI to a resource or subject of RDF statements
	 * @return	For named (non-anonymous) types, return a Collection of solely the type itself;
	 * 			for anonymous types, return a Collection of all terms used to define the type
	 * 			(.e.g, terms used within owl:Restriction, etc.).
	 */
	public Collection<URI> getTypeSignature(URI type);
}
