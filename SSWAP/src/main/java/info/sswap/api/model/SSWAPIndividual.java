/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;

/**
 * Represents an individual in SSWAP, which corresponds to an RDF resource (a
 * URI or blank node). Individuals can have properties (both object properties
 * and datatype properties), which in turn have values. An individual is always
 * in reference to a document; to create a new individual, see {@link
 * SSWAPDocument}.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public interface SSWAPIndividual extends SSWAPElement {
	/**
	 * Checks whether this node is a blank node (i.e., it does not have its own URI, but rather it is represented by a
	 * blank node in the RDF). 
	 * 
	 * @return true if this is a blank node, false otherwise.
	 */
	public boolean isAnonymous();
	
	/**
	 * Gets all declared types of the individual.
	 * 
	 * @return a collection of declared types for this individual
	 */
	public Collection<SSWAPType> getDeclaredTypes();

	/**
	 * Gets a single type for this individual that summarizes all the declared types. If the individual contains one declared type,
	 * this is the type that will be returned. If the individual contains more than one declared type,
	 * it will return an anonymous type that is an intersection of all of the declared types.
	 * If the individual does not have any declared type, it returns owl:Thing.
	 * 
	 * @return a single type that represents all declared types for this individual
	 */
	public SSWAPType getDeclaredType();

	/**
	 * Gets all types for this individual (including both explicitly declared types and inferred types).
	 * 
	 * @return a collection of all types for this individual (including explicitly declared types and inferred ones)
	 */
	public Collection<SSWAPType> getTypes();


	/**
	 * Gets a single type for this individual that summarizes all the types for this individual (both explicitly declared and inferred).
	 * The type returned by this method is an anonymous type that is an intersection of all the types.
	 * 
	 * @return a single type that represents all the types for this individual
	 */
	public SSWAPType getType();
	
	
	/**
	 * Adds a new declared type to the individual
	 * 
	 * @param type
	 *            the type to be added
	 */
	public void addType(SSWAPType type);

	/**
	 * Removes a type from the individual. If the passed type is not a current type of this individual, this method does
	 * nothing.
	 * 
	 * @param type
	 *            the type to be removed.
	 */
	public void removeType(SSWAPType type);

	/**
	 * Gets all the properties for this individual.
	 * 
	 * @return the set of the properties.
	 */
	public Collection<SSWAPProperty> getProperties();

	/**
	 * Gets the property by its Predicate. If there are multiple values for the predicate, this method return only the first
	 * value.
	 * 
	 * @param predicate the predicate for which property should be retrieved
	 *            
	 * @return the property with this predicate, or null if there are no values for this predicate
	 */
	public SSWAPProperty getProperty(SSWAPPredicate predicate);

	/**
	 * Gets all the properties (and their values) for the predicate
	 * 
	 * @param predicate
	 *            the predicate for which the properties should be retrieved
	 * @return the collection of the properties with this predicate, or null if there are no such properties
	 */
	public Collection<SSWAPProperty> getProperties(SSWAPPredicate predicate);

	/**
	 * Removes a single property instance from this individual (identified by the
	 * specified SSWAPProperty instance). If the individual has other values for
	 * this property, these entries are unaffected (as opposed to
	 * clearProperty(SSWAPPredicate)). If the specified property does not exist
	 * for this individual, this method does nothing.
	 * 
	 * @param property
	 *            property and its value to be removed
	 * @see #removeProperty(SSWAPPredicate, SSWAPElement)
	 * @see #clearProperty(SSWAPPredicate)
	 */
	public void removeProperty(SSWAPProperty property);
	
	/**
	 * Removes a single property from this individual (property is identified by the predicate and the value) 
	 * 
	 * @param predicate the predicate for the property to be removed
	 * @param value the value for the property to be removed
	 */
	public void removeProperty(SSWAPPredicate predicate, SSWAPElement value);
	
	/**
	 * Removes all property instances of the specified predicate. 
	 * 
	 * @param predicate the predicate whose all values should be removed
	 * @see #removeProperty(SSWAPProperty)
	 */
	public void clearProperty(SSWAPPredicate predicate);
	
	/**
	 * Checks whether this individual is of a given type.
	 * 
	 * @param type the type to be checked
	 * @return true if this individual is of given type, false otherwise
	 */
	public boolean isOfType(SSWAPType type);
	
	/**
	 * Checks whether this individual is compatible with the argument type;
	 * i.e., if the individual is asserted to be of this type (e.g., via
	 * addType(SSWAPType) method), would the ontology remain consistent or
	 * become inconsistent?
	 * 
	 * @param type
	 *            type whose compatibility with this individual should be
	 *            checked
	 * @return true if the type is compatible, false otherwise
	 */
	public boolean isCompatibleWith(SSWAPType type);
	
	/**
	 * Adds a property to this individual with the specified individual as the
	 * value.
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param individual
	 *            the value for the property
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the individual is not legal for the predicate (e.g., an
	 *             object for a datatype property)
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, SSWAPIndividual individual) throws IllegalArgumentException;
	
	/**
	 * Adds a property to this individual with the specified value. For
	 * predicates with a declared <code>rdfs:range</code>, the system will tag
	 * the value with the appropriate datatype.
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param value
	 *            the literal value for the property
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the value is not legal for the predicate (e.g., a literal
	 *             for an object property)
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, String value) throws IllegalArgumentException;
	
	/**
	 * Adds a property to this individual with the specified literal value with
	 * the specified datatype.
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param value
	 *            the literal value for the property
	 * @param datatype
	 *            URI for typing the literal value (<i>e.g.</i>,
	 *            {@link SSWAPDatatype.XSD#anyURI})
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the value or datatype is not legal for the predicate
	 *             (e.g., a literal for an object property)
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, String value, URI datatype) throws IllegalArgumentException;
	
	
	/**
	 * Adds a property to this individual with the specified literal value
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param literal
	 *            the literal value for the property
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the literal is not legal for the predicate (e.g., a
	 *             literal for an object property)
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, SSWAPLiteral literal) throws IllegalArgumentException;
	
	/**
	 * Sets the value of the property to the individual. Setting the value
	 * removes any other values the current individual may have for the
	 * specified predicate.
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param individual
	 *            the individual
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the individual is not legal for the predicate (e.g., an
	 *             object for a datatype property)
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, SSWAPIndividual individual) throws IllegalArgumentException;
	
	/**
	 * Sets the value of the property to the specified value. Setting the value
	 * removes any other values the current individual may have for the
	 * predicate. For predicates with a declared <code>rdfs:range</code>, the
	 * system will tag the value with the appropriate datatype.
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param value
	 *            the literal value
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the value is not legal for the predicate (e.g., a literal
	 *             for an object property)
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, String value) throws IllegalArgumentException;
	
	/**
	 * Sets the value of the property to the specified value and datatype URI.
	 * Setting the value removes any other values the current individual may
	 * have for the predicate.
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param value
	 *            the literal value
	 * @param datatype
	 *            URI for typing the literal value (<i>e.g.</i>,
	 *            {@link SSWAPDatatype.XSD#anyURI})
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the value or datatype is not legal for the predicate
	 *             (e.g., a literal for an object property)
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, String value, URI datatype) throws IllegalArgumentException;
	
	/**
	 * Sets the value of the property to the literal object. Setting the value
	 * removes any other values the current individual may have for the
	 * predicate.
	 * 
	 * @param predicate
	 *            the predicate for the added property
	 * @param literal
	 *            value for the property
	 * @return the newly created property
	 * @throws IllegalArgumentException
	 *             if the literal is not legal for the predicate (e.g., a
	 *             literal for an object property)
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, SSWAPLiteral literal) throws IllegalArgumentException;
	
	/**
	 * Checks whether the individual has a property with the specified value for the predicate.
	 * 
	 * @param predicate the predicate for the property
	 * @param element the value for the property
	 * @return true, if there is such a property for this individual, false otherwise
	 */
	public boolean hasValue(SSWAPPredicate predicate, SSWAPElement element);
	
	/**
	 * Retrieves all properties of the individual with the specified value (regardless of the predicate
	 * of these properties)
	 * 
	 * @param element the value for properties to be returned
	 * @return a collection of properties with the specified value (may be empty but never null)
	 */
	public Collection<SSWAPProperty> hasValue(SSWAPElement element);
	
	/**
	 * Retrieves an inferred view (a copy) of the individual. The view contains
	 * both explicitly asserted and inferred properties and types, so that they
	 * can be retrieved by methods such as getProperty(), etc. This view is a
	 * snapshot of inferences and is computed at the time of invocation of this
	 * method (i.e., it is not updated if this individual's properties change;
	 * for an updated version of the inferred model, this method has to be
	 * invoked again).
	 * 
	 * @return an inferred view of this individual
	 */
	public SSWAPIndividual getInferredIndividual();
}
