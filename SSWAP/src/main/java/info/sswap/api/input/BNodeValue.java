/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;


/**
 * Represents a bnode value.
 * 
 * @author Evren Sirin
 */
public interface BNodeValue extends InputValue {
	/**
	 * Get the bnode ID for this value.
	 * @return the bnode ID for this value.
	 */
	public String getID();	
}
