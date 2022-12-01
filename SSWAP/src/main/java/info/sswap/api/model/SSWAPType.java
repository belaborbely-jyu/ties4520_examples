/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;

/**
 * Represents a type of an individual in SSWAP.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface SSWAPType extends SSWAPElement {

	/**
	 * Creates and returns a new type that is a union of this type and the given type. (That is, the new type contains
	 * all the individuals that belong to this type, the other type, OR both).
	 * 
	 * @param a
	 *            the other type, with which the union is taken.
	 * @return the new type that is the union of the current type and the given type.
	 */
	public SSWAPType unionOf(SSWAPType a);

	/**
	 * Creates and returns a new type that is an intersection of this type and the given type. (That is, the new type
	 * contains all the individuals that BOTH belong to this type AND the other type.)
	 * 
	 * @param a
	 *            the other type, with which the intersection is taken
	 * @return the new type that is the intersection of the current type and the given type.
	 */
	public SSWAPType intersectionOf(SSWAPType a);

	/**
	 * Creates the complement of the current type. (That is, the new type contains all the individuals that do NOT belong
	 * to the current type.)
	 * 
	 * @return the new type that is the complement of the current type.
	 */
	public SSWAPType complementOf();
	
	/**
	 * Adds an owl:allValuesFrom restriction to this type on the specified predicate
	 * @param predicate predicate to which the restriction applies
	 * @param type the argument for owl:allValuesFrom
	 */
	public void addRestrictionAllValuesFrom(SSWAPPredicate predicate, SSWAPType type);
	
	/**
	 * Adds an owl:someValuesFrom restriction to this type on the specified predicate
	 * @param predicate predicate to which the restriction applies
	 * @param type the argument for owl:someValuesFrom
	 */
	public void addRestrictionSomeValuesFrom(SSWAPPredicate predicate, SSWAPType type);
	
	/**
	 * Adds an owl:hasValue restriction to this type on the specified predicate
	 * @param predicate predicate to which the restriction applies
	 * @param element the value for owl:hasValue
	 */
	public void addRestrictionHasValue(SSWAPPredicate predicate, SSWAPElement element);
	
	/**
	 * Adds an owl:hasSelf restriction to this type on the specified predicate
	 * @param predicate predicate to which the restriction applies
	 * @param value a boolean value (argument to owl:hasSelf)
	 */
	public void addRestrictionHasSelf(SSWAPPredicate predicate, boolean value);
	
	/**
	 * Adds a min cardinality restriction to this type on the specified predicate.
	 * 
	 * @param predicate predicate to which the cardinality restriction applies
	 * @param minCardinality the value of the minCardinality
	 */
	public void addRestrictionMinCardinality(SSWAPPredicate predicate, int minCardinality);
	
	/**
	 * Adds a max cardinality restriction to this type on the specified predicate.
	 * 
	 * @param predicate predicate to which the cardinality restriction applies
	 * @param maxCardinality the value of the maxCardinality
	 */
	public void addRestrictionMaxCardinality(SSWAPPredicate predicate, int maxCardinality);
	
	/**
	 * Adds a cardinality restriction to this type on the specified predicate.
	 * 
	 * @param predicate predicate to which the cardinality restriction applies
	 * @param cardinality the cardinality value
	 */
	public void addRestrictionCardinality(SSWAPPredicate predicate, int cardinality);
	
	/**
	 * Adds an rdfs:subClassOf axiom to this type.
	 * 
	 * @param type the super type
	 */
	public void addSubClassOf(SSWAPType type);
	
	/**
	 * Adds an owl:equivalentClass axiom to this type.
	 * 
	 * @param type the equivalent type
	 */
	public void addEquivalentClass(SSWAPType type);
	
	/**
	 * Adds an owl:disjointWith axiom to this type.
	 * 
	 * @param type the disjoint type with this one
	 */
	public void addDisjointWith(SSWAPType type);
	
	/**
	 * Adds an owl:intersectionOf axiom to this type.
	 * 
	 * @param classes SSWAPTypes that compose the intersection
	 */
	public void addIntersectionOf(Collection<SSWAPType> classes);
	
	/**
	 * Adds an owl:unionOf axiom to this type 
	 * @param classes SSWAPTypes that compose the union
	 */
	public void addUnionOf(Collection<SSWAPType> classes);
	
	/**
	 * Adds an owl:disjointUnionOf axiom to this type
	 * @param disjointClasses SSWAPTypes that compose the disjointedness
	 */
	public void addDisjointUnionOf(Collection<SSWAPType> disjointClasses);
	
	/**
	 * Adds an owl:oneOf axiom to this type
	 * @param oneOf the collection of URIs that will be converted into argument to owl:oneOf
	 */
	public void addOneOf(Collection<URI> oneOf);
	
	/**
	 * Annotates a type with the given annotation predicate.
	 * 
	 * @param predicate the predicate to be used in annotation
	 * @value the value of the annotation predicate
	 */
	public void addAnnotationPredicate(SSWAPPredicate predicate, SSWAPElement value);
	
	/**
	 * Checks whether this type is a subtype of the other. (That is, whether all individuals of this type are
	 * necessarily individuals of the other type). By the laws of subsumption, "subtype" includes equivalency,
	 * so a type is always a subtype of itself.
	 * 
	 * @param superType
	 *            the potential super type
	 * @return true if this is a subtype of superType
	 */
	public boolean isSubTypeOf(SSWAPType superType);
	
	/**
	 * Checks whether this type is a strict subtype of the other. (That is, whether all individuals of this type are
	 * necessarily individuals of the other type, yet this type is not equivalent to the other type).
	 * 
	 * @param superType
	 *            the potential super type
	 * @return true if this is a strict subtype of sup.
	 */
	public boolean isStrictSubTypeOf(SSWAPType superType);
	
	/**
	 * Checks whether this type is unsatisfiable (i.e., whether it is a sub type of owl:Nothing).
	 * 
	 * @return true if this type is unsatisfiable, false otherwise
	 */
	public boolean isNothing();
	
	/**
	 * Checks whether the given type belongs to restricted vocabulary 
	 * (e.g., types defined in RDF, RDFS, OWL or SSWAP namespaces).
	 * 
	 * @return true if the type belongs to restricted vocabulary.
	 */
	public boolean isReserved();
}
