/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;

/**
 * Represents an intersection type where valid input values should satisfy the requirements of every nested input type.
 * 
 * @author Evren Sirin
 */
public interface IntersectionInput extends NaryInput {
	/**
	 * Returns <code>owl:intersectionOf</code>
	 */
	public URI getType();
}
