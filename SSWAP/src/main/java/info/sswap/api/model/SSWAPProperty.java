/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * A <code>SSWAPProperty</code> is a specific instance of a
 * {@link SSWAPPredicate} with an assigned value, associated with a
 * {@link SSWAPIndividual}. A property consists of a name (URI) and a non-null
 * value ({@link SSWAPElement}). Like <code>String</code>, a
 * <code>SSWAPProperty</code> is immutable. To create a
 * <code>SSWAPProperty</code>, assign a
 * property/value pair to an individual; see {@link SSWAPIndividual}.
 * 
 * @see SSWAPElement
 * @see SSWAPIndividual
 * @see SSWAPPredicate
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface SSWAPProperty extends SSWAPElement {
	/**
	 * Gets the value of the property.
	 * 
	 * @return the SSWAPElement (dereferenced) containing the value.
	 */
	public SSWAPElement getValue();
	
	/**
	 * Gets the SSWAPPredicate for this property. 
	 * 
	 * @return SSWAPPredicate for this property
	 */
	public SSWAPPredicate getPredicate();
	
	/**
	 * Gets the individual for which this property is assigned. If the property
	 * is no longer is associated with an individual (e.g., it was removed),
	 * this method returns null.
	 * 
	 * @return the individual or null (if the property is no longer associated
	 *         with an individual)
	 */
	public SSWAPIndividual getIndividual();
	
	/**
	 * Removes the property from the individual.
	 */
	public void removeProperty();
}
