/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;

/**
 * Represents a union type where valid input values should satisfy the one of the nested input types.
 * 
 * @author Evren Sirin
 */
public interface UnionInput extends NaryInput {
	/**
	 * Returns <code>owl:unionOf</code>
	 */
	public URI getType();

	/**
	 * Returns the index of the nested input which will determine the value of this union or <code>-1</code> if no value is
	 * provided for this union.
	 */
	public int getValueIndex();
	
	/**
	 * Returns the index of the input where value of this union should be read or <code>-1</code> if no value is
	 * provided for this union.
	 */
	public void setValueIndex(int valueIndex);
	
	public URI getValueType(int valueIndex);
	
	public void setValueType(int valueIndex, URI valueType);
}
