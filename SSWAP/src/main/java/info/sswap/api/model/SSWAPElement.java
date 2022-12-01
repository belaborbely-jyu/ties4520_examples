/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * Represents a basic element in a {@link SSWAPDocument}.
 * 
 * @see SSWAPDocument
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public interface SSWAPElement extends SSWAPModel {
	/**
	 * Checks whether this element is a literal.
	 * 
	 * @return true if the element is a literal
	 */
	public boolean isLiteral();

	/**
	 * Checks whether this element is a SSWAPIndividual.
	 * 
	 * @return true if the element is a SSWAPIndividual.
	 */
	public boolean isIndividual();

	/**
	 * Checks whether this element is a list of SSWAPElements.
	 * 
	 * @return true if the element is a list
	 */
	public boolean isList();

	/**
	 * Type-safe cast of this element to a String. This is only possible for elements that are literals.
	 * 
	 * @return a string, if the element is a literal, or null otherwise.
	 */
	public String asString();

	/**
	 * Type-safe cast of this element to an integer. This is only possible for elements that are literals, and they
	 * contain a valid integer data.
	 * 
	 * @return an integer, if the element is a literal containing a legal integer, or null otherwise.
	 */
	public Integer asInteger();

	/**
	 * Type-safe cast of this element to a double. This is only possible for elements that are literals, and they
	 * contain a valid numerical data.
	 * 
	 * @return a double, if the element is a literal containing a legal double, or null otherwise.
	 */
	public Double asDouble();

	/**
	 * Type-safe case of this element to a boolean. This is only possible for elements that are literals and they
	 * represent a valid boolean value. The only valid boolean values are "true" and "false" (case insensitive).
	 * 
	 * @return a boolean, if the element is a literal containing a valid boolean value (as defined above), or null
	 *         otherwise.
	 */
	public Boolean asBoolean();

	/**
	 * Type-safe cast of this element to a SSWAPIndividual. This is only possible for elements that are SSWAPIndividuals.
	 * 
	 * @return a SSWAPIndividual, if the element is actually a SSWAPIndividual, or null otherwise.
	 */
	public SSWAPIndividual asIndividual();
	
	/**
	 * Type-safe cast of this element to SSWAPLiteral. This is only possible for elements that are SSWAPLiterals.
	 * 
	 * @return a SSWAPLiteral, if the element is actually a SSWAPLiteral, or null otherwise
	 */
	public SSWAPLiteral asLiteral();

	/**
	 * Type-safe cast of this element to a list. This is only possible for elements that are lists.
	 * 
	 * @return a SSWAPList, if the element is actually a list, or null otherwise.
	 */
	public SSWAPList asList();
	
	/**
	 * Adds an rdfs:label statement to this type.
	 * 
	 * @param label the label to be added
	 */
	public void addLabel(String label);

	/**
	 * Adds and rdfs:comment to this type.
	 * 
	 * @param comment the comment to be added to this type
	 */
	public void addComment(String comment);
		
	/**
	 * Returns that rdfs:label value of this type or <code>null</code> if no label exists. If multiple labels exist for
	 * the type any one of them is returned. For literal labels, only the lexical form is returned not the type nor the 
	 * language identifier.
	 * 
	 * @return label of this type or <code>null</code> if no label exists
	 */
	public String getLabel();

	/**
	 * Returns that rdfs:comment value of this type or <code>null</code> if no comment exists. If multiple comments
	 * exist for the type any one of them is returned. For literal comments, only the lexical form is returned not the
	 * type nor the language identifier.
	 * 
	 * @return comment of this type or <code>null</code> if no comment exists
	 */
	public String getComment();
}
