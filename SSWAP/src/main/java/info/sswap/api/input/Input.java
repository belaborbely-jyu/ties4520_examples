/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;

/**
 * Represents the input specification for an OWL class or datatype expression. The purpose of this presentation is to
 * provide an easy way to create an input form for the associated class expression. The hierarchy of this class is based
 * on OWL expressions but is significantly simplified to make input form creation easy.
 * 
 * @author Evren Sirin
 */
public interface Input {
	/**
	 * Returns the type of the input.
	 */
	public URI getType();

	/**
	 * Returns <code>true</code> if there are no restrictions on the input. This is the case for <code>owl:Thing</code>
	 * and <code>rdfs:Literal</code>.
	 */
	public boolean isUnrestricted();

	/**
	 * Returns the value associated with this input object.
	 */
	public InputValue getValue();

	/**
	 * Sets the value for this input object.
	 */
	public void setValue(InputValue value);

	/**
	 * Returns the label of this input. The label of the input is application-dependent. Typically, if the input
	 * represent a class label will be the <code>rdfs:label</code> for that class. If the input is a property input,
	 * label of the property is used. If there is no <code>rdfs:label</code> for the class or property then the local
	 * name of the URI is used.
	 * 
	 * @return the label for this input or <code>null</code> if there is no label
	 */
	public String getLabel();

	/**
	 * Sets the label for this input.
	 */
	public void setLabel(String label);

	/**
	 * Returns the description of this input.The description of the input is application-dependent. Typically, if the
	 * input represent a class label will be the <code>rdfs:comment</code> for that class. If the input is a property
	 * input, comment of the property is used.
	 * 
	 * @return the description of this input or <code>null</code> if there is no description
	 */
	public String getDescription();

	/**
	 * Sets the description of this input.
	 */
	public void setDescription(String description);

	/**
	 * Visitor function.
	 */
	public void accept(InputVisitor visitor);
	
	public PropertyInput getPropertyInput();
	public void setPropertyInput(PropertyInput propertyInput);
}
