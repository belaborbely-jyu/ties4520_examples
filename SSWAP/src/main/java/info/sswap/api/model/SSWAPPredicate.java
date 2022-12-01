/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.util.Collection;

/**
 * Represents an RDF predicate in SSWAP. Predicates are distinct from
 * properties, in that a predicate refers to the term itself, while a property
 * is an instance of a predicate on an individual with an associated value (see
 * {@link SSWAPProperty}). A <code>SSWAPPredicate</code> contains global
 * statements, along with methods to manipulate
 * <code>owl:DatatypeProperty</code> or <code>owl:ObjectProperty</code>
 * definitions (<i>e.g.</i>, adding their domains, ranges etc.).
 * <p>
 * Usually, a <code>SSWAPPredicate</code> is not created <i>de novo</i>; it is
 * read from the Internet via {@link SSWAPDocument#getPredicate(java.net.URI)}.
 * 
 * @see SSWAPDocument
 * @see SSWAPProperty
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public interface SSWAPPredicate extends SSWAPElement {
	
	/**
	 * Adds the type information to this property (i.e., whether this is a DatatypeProperty, an ObjectProperty, FunctionalProperty, InverseProperty etc.)
	 * @param type the type to be added.
	 */
	public void addType(SSWAPType type);
	
	/**
	 * Adds an rdfs:subPropertyOf axiom to this type
	 * 
	 * @param predicate the super predicate type
	 */
	public void addSubPredicateOf(SSWAPPredicate predicate);
	
	/**
	 * Adds an rdfs:domain axiom to this predicate
	 * @param type the domain of this predicate
	 */
	public void addDomain(SSWAPType type);

	/**
	 * Adds an rdfs:range axiom to this predicate
	 * @param type the domain of this predicate
	 */
	public void addRange(SSWAPType type);
	
	/**
	 * Adds an rdfs:range axiom to this predicate
	 * @param datatype the domain of this predicate
	 */
	public void addRange(SSWAPDatatype datatype);

	
	/**
	 * Adds an owl:equivalentProperty axiom to this predicate
	 * 
	 * @param predicate the other equivalent predicate
	 */
	public void addEquivalentPredicate(SSWAPPredicate predicate);
	
	/**
	 * Adds owl:inverseOf axiom to this predicate
	 * 
	 * @param predicate the inverse predicate to this one
	 */
	public void addInverseOf(SSWAPPredicate predicate);
	
	/**
	 * Annotates a predicate with the given annotation predicate.
	 * 
	 * @param predicate the predicate to be used in annotation
	 * @value the value of the annotation predicate
	 */
	public void addAnnotationPredicate(SSWAPPredicate predicate, SSWAPElement value);
	
	/**
	 * Checks whether this predicate is a sub predicate of other. 
	 * 
	 * @param sup the potential super predicate
	 * @return true if this is a subpredicate of sup
	 */
	public boolean isSubPredicateOf(SSWAPPredicate sup);
	
	/**
	 * Checks whether this predicate is a strict sub predicate of the other.
	 * 
	 * @param sup the potential strict super predicate
	 * @return true if this is a strict sub predicate of sup
	 */
	public boolean isStrictSubPredicateOf(SSWAPPredicate sup);
	
	/**
	 * Gets the type for the range for an object predicate. If the predicate has more than 
	 * one type for the range, this method will return an intersection of all these types.
	 * If the range is not defined, this method will return owl:Thing.
	 * 
	 * @throws IllegalArgumentException if this predicate is not an object predicate
	 * @return the type for the range for an object predicate 
	 */
	public SSWAPType getObjectPredicateRange() throws IllegalArgumentException;
	
	/**
	 * Gets all the types for the range for an object predicate.
	 * 
	 * @throws IllegalArgumentException if this predicate is not an object predicate
	 * @return the collection of types for the object predicate
	 */
	public Collection<SSWAPType> getObjectPredicateRanges() throws IllegalArgumentException;
	
	/**
	 * Gets the datatype for the range for a datatype predicate. In the rare case, when 
	 * a datatype predicate has more than one range, this method will only return the first 
	 * datatype. To retrieve all datatypes use getDatatypePredicateRanges().
	 * 
	 * @throws IllegalArgumentException if this predicate is not a datatype predicate
	 * @return the URI of the datatype or null, if not known
	 */
	public String getDatatypePredicateRange() throws IllegalArgumentException;

	/**
	 * Gets all the datatypes for the range for a datatype predicate.
	 * 
	 * @throws IllegalArgumentException if this predicate is not a datatype predicate
	 * @return collection of URIs for the range of the datatype predicate
	 */
	public Collection<String> getDatatypePredicateRanges() throws IllegalArgumentException;
	
	/**
	 * Retrieves information about the domain of the specified predicate. If the domain of the predicate
	 * has more than one type, this method will return an intersection of all the types in the domain.
	 * If the domain is not defined, this method will return owl:Thing.
	 * 
	 * @return the type for the domain of this predicate
	 */
	public SSWAPType getDomain();
	
	/**
	 * Checks whether the given predicate is defined as an object predicate
	 * 
	 * @return true if the predicate is defined as an object predicate
	 */
	public boolean isObjectPredicate();

	/**
	 * Checks whether the given predicate is defined as a datatype predicate
	 * 
	 * @return true if the predicate is defined as a datatype predicate
	 */
	public boolean isDatatypePredicate();
	
	/**
	 * Checks whether the given predicate is defined as an annotation predicate
	 * 
	 * @return true if the predicate is defined as an annotation predicate
	 */
	public boolean isAnnotationPredicate();
	
	/**
	 * Checks whether the given predicate belongs to restricted vocabulary 
	 * (e.g., predicates defined in RDF, RDFS, OWL or SSWAP namespaces).
	 * 
	 * @return true if the predicate belongs to restricted vocabulary.
	 */
	public boolean isReserved();

}
