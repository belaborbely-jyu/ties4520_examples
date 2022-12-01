/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;

/**
 * Represents a document in SSWAP that holds RDF content, such as may be hosted
 * on the web (<i>e.g.</i>, an <code>RDG</code>). A <code>SSWAPDocument</code>
 * may contain elements such as individuals and properties, or define types or
 * predicates. Instances of <code>SSWAPDocument</code>s can be read or created
 * by the main {@link SSWAP} class.
 * 
 * @see SSWAP
 * @see SSWAPElement
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface SSWAPDocument extends SSWAPModel {
	/**
	 * Gets a SSWAPType for an URI.
	 * 
	 * @param uri
	 *            the URI of the type
	 * @return a SSWAPType for the URI.
	 */
	public SSWAPType getType(URI uri);
	
	/**
	 * Creates anonymous type.
	 * @return an anonymous type
	 */
	public SSWAPType createAnonymousType();

	/**
	 * Creates an anonymous SSWAPIndividual (blank node). This method can be
	 * used to create a custom individual (i.e., other than the SSWAPNodes like
	 * Subject, Object, Resource etc.)
	 * 
	 * @return the created individual
	 */
	public SSWAPIndividual createIndividual();
	
	/**
	 * Creates a SSWAPIndividual representation of a specific resource (URI).
	 * This method can be used to create a custom individual (i.e., other than
	 * the SSWAPNodes like Subject, Object, Resource etc.)
	 * 
	 * @param uri
	 *            the URI of the individual to be created; may be null,
	 *            in which case an anonymous individual is created
	 * @return the created individual
	 */
	public SSWAPIndividual createIndividual(URI uri);

	/**
	 * Creates an empty list. 
	 * 
	 * @return a list.
	 */
	public SSWAPList createList();
	
	/**
	 * Creates a literal with the specified value.
	 * 
	 * @param value
	 *            the value of the literal
	 * @return the created literal
	 */
	public SSWAPLiteral createLiteral(String value);

	/**
	 * Creates a SSWAPElement that is a typed literal with the specified value.
	 * 
	 * @param value
	 *            the value of the literal
	 * @param datatypeURI
	 *            the URI of the datatype
	 * @return the created typed literal
	 * @throws IllegalArgumentException if the value is not valid according to the declared datatype URI
	 */
	public SSWAPLiteral createTypedLiteral(String value, URI datatypeURI) throws IllegalArgumentException;
	
	/**
	 * Creates a SSWAPType in the specified model that is an intersection of the given types. 
	 * 
	 * @param types the types
	 * @return an anonymous type that is an intersection of the the passed types
	 */
	public SSWAPType createIntersectionOf(Collection<SSWAPType> types);
	
	/**
	 * Creates a SSWAPType in the specified model that is a union of the given types.
	 * 
	 * @param types the types
	 * @return an anonymous type that is a union of the given types.
	 */
	public SSWAPType createUnionOf(Collection<SSWAPType> types);
	
	/**
	 * Gets a SSWAPPredicate object for the given property URI.
	 * A non-dereferencable URI will "create" a predicate.
	 * 
	 * @param uri the URI of the property
	 * @return the SSWAPPredicate object
	 */
	public SSWAPPredicate getPredicate(URI uri);

	/**
	 * Creates an anonymous SSWAPDatatype
	 * @return the datatype
	 */
	public SSWAPDatatype createAnonymousDatatype();
	
	/**
	 * Gets a named datatype object
	 * @param uri the URI of the datatype
	 * @return the datatype
	 */
	public SSWAPDatatype getDatatype(URI uri);
	
	/**
	 * Creates a new, anonymous individual, and populates it with data from the
	 * <code>sourceIndividual</code> (essentially clones the source individual).
	 * This method is equivalent to
	 * <code>newIndividual(sourceIndividual,null)</code>;
	 * 
	 * @see #newIndividual(SSWAPIndividual, URI)
	 * 
	 * @param <T>
	 *            the type of the individual
	 * @param sourceIndividual
	 *            the individual to be used as a source of data for populating
	 *            the new individual
	 * @throws IllegalArgumentException
	 *             on an attempt to copy a SSWAPNode into a generic
	 *             SSWAPDocument that is not a SSWAPProtocol document
	 * @return a clone of the individual (an anonymous individual)
	 */
	public <T extends SSWAPIndividual> T newIndividual(T sourceIndividual) throws IllegalArgumentException;

	/**
	 * Copies the data from the <code>sourceIndividual</code> into the
	 * individual whose URI is passed as <code>targetURI</code>. If there is not
	 * yet an individual with such a URI, it is created. If
	 * <code>targetURI</code> is null, an anonymous individual is created.
	 * <p>
	 * Notes:
	 * <ol>
	 * <li>The copy of the <code>sourceIndividual</code> is a deep copy;
	 * <i>i.e.</i>, if there are any object properties with anonymous
	 * individuals as values, a deep copy is performed on those individuals
	 * recursively. If there are any object properties with named individuals as
	 * values, those named individual are not copied.</li>
	 * <li>The data being copied includes only types of the individual (
	 * <code>SSWAPType</code>) and its properties (<code>SSWAPProperty</code>).
	 * In particular, associations with <code>SSWAPNodes</code> are not copied
	 * (<i>e.g.</i>, if a <code>SSWAPSubject</code> is being copied, the copy
	 * will not contain references to <code>SSWAPObjects</code> or
	 * <code>SSWAPGraphs</code>; if desired, these must be made explicitly).</li>
	 * <li>Copying across documents is supported (<i>i.e.</i>, passing an
	 * individual from a different <code>SSWAPDocument</code> to this method) --
	 * a deep copy will be made across the documents, including named
	 * individuals. The restriction specified in (2) still holds (<i>i.e.</i>,
	 * references to <code>SSWAPNodes</code> are not copied across documents).</li>
	 * <li><code>SSWAPNodes</code> (<i>e.g.</i>, <code>SSWAPResource</code>,
	 * <code>SSWAPSubject</code>, <code>SSWAPObject</code>) are supported only
	 * in <code>SSWAPProtocol</code> documents (<i>e.g.</i>, <code>RDG</code>,
	 * <code>RIG</code>, <code>RRG</code>, <code>RQG</code>). An attempt to copy
	 * them into a generic <code>SSWAPDocument</code> (<i>e.g.</i>, a document
	 * containing a definition of <code>SSWAPType</code> or
	 * <code>owl:Class</code>) throws an <code>IllegalArgumentException</code>.</li>
	 * <li>Copying types and properties from one individual to another can
	 * create logical inconsistencies. The usual precautions when adding types
	 * and properties should be observed.</li>
	 * </ol>
	 * <p>
	 * 
	 * @param <T>
	 *            the type of the individual
	 * @param sourceIndividual
	 *            the individual with the data to be copied
	 * @param targetURI
	 *            the URI of the new individual; may be null for an anonymous
	 *            individual
	 * @throws IllegalArgumentException
	 *             on an attempt to copy a SSWAPNode into a generic
	 *             SSWAPDocument that is not a SSWAPProtocol document
	 * @return new individual (with the specified uri)
	 */
	public <T extends SSWAPIndividual> T newIndividual(T sourceIndividual, URI targetURI) throws IllegalArgumentException;
}
